package org.ecocean.identity;

import java.util.List;
import java.io.File;
import java.io.IOException;
import org.ecocean.Shepherd;
import org.ecocean.Encounter;
import org.ecocean.Annotation;
import org.ecocean.Util;
import org.ecocean.servlet.ServletUtilities;
import org.ecocean.CommonConfiguration;
import org.ecocean.media.MediaAsset;
import org.ecocean.media.MediaAssetFactory;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.lang3.StringUtils;

/*
import org.ecocean.ImageAttributes;
import org.ecocean.Util;
import org.ecocean.Shepherd;
import org.ecocean.Encounter;
import org.ecocean.Occurrence;
import org.ecocean.MarkedIndividual;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import org.ecocean.media.*;
import org.ecocean.RestClient;
import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import org.joda.time.DateTime;
import org.apache.commons.lang3.StringUtils;
*/
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.io.FileWriter;
import java.io.BufferedWriter;
import javax.jdo.Extent;
import javax.jdo.Query;


public class BenWhiteshark {
//#BenWhitesharkJobStartDirectory = /efs/job/start
//#BenWhitesharkJobResultsDirectory = /efs/job/results

    public static final String ERROR_KEY = "__ERROR__";
    public static final String SERVICE_NAME = "BenWhiteshark";

    public static boolean enabled() {
        return ((getJobStartDir() != null) && (getJobResultsDir() != null));
    }
    public static JSONObject iaStatus() {
        JSONObject j = new JSONObject();
        j.put("enabled", enabled());
        return j;
    }

    //right now we use .isExemplar on Annotations; but later we may shift to some other logic, including (discussed with ben):
    //  quality keywords on image, features approved/input by manual method (e.g. end-points of fin) etc....
    //  also: the choice to focus on Annotation vs MediaAsset feels a little arbitrary; am choosing Annotation... for now?
    public static List<Annotation> getExemplars(Shepherd myShepherd) {
        Extent all = myShepherd.getPM().getExtent(Annotation.class, true);
        Query qry = myShepherd.getPM().newQuery(all, "isExemplar");
        Collection results = (Collection)qry.execute();
        List<Annotation> rtn = new ArrayList<Annotation>();
        for (Object o : results) {
            Annotation ann = (Annotation)o;
            if (ann.getMediaAsset() != null) rtn.add(ann);
        }
        return rtn;
    }

    public static File getJobStartDir() {
        String d = CommonConfiguration.getProperty("BenWhitesharkJobStartDirectory", "context0");
        if (d == null) return null;
        return new File(d);
    }
    public static File getJobResultsDir() {
        String d = CommonConfiguration.getProperty("BenWhitesharkJobResultsDirectory", "context0");
        if (d == null) return null;
        return new File(d);
    }

    //TODO support taxonomy!
    public static String startJob(List<MediaAsset> queryMAs, Shepherd myShepherd) {
        List<Annotation> exs = getExemplars(myShepherd);
        if ((exs == null) || (exs.size() < 1)) throw new RuntimeException("getExemplars() returned no results");
        List<MediaAsset> tmas = new ArrayList<MediaAsset>();
        for (Annotation ann : exs) {
            if (!queryMAs.contains(ann.getMediaAsset())) tmas.add(ann.getMediaAsset());
        }
        String[] ids = new String[queryMAs.size()];
        for (int i = 0 ; i < queryMAs.size() ; i++) {
            ids[i] = Integer.toString(queryMAs.get(i).getId());
        }
        String taskId = startJob(queryMAs, tmas);
        IdentityServiceLog log = new IdentityServiceLog(taskId, ids, SERVICE_NAME, null, new JSONObject("{\"_action\": \"initIdentify\"}"));
        log.save(myShepherd);
        return taskId;
    }
    //single queryMA convenience method
    public static String startJob(MediaAsset queryMA, Shepherd myShepherd) {
        List<MediaAsset> mas = new ArrayList<MediaAsset>();
        mas.add(queryMA);
        return startJob(mas, myShepherd);
    }
    public static String startJob(List<MediaAsset> queryMAs, List<MediaAsset> targetMAs) {
        String taskId = Util.generateUUID();
        String contents = "";
        for (MediaAsset ma : queryMAs) {
            contents += jobdata(ma);
        }
        contents += "-1\t-1\t-1\t-1\t-1\t-1\t-1\t-1\n";   //agreed divider between queryMA(s) and targetMA(s)
        for (MediaAsset ma : targetMAs) {
            contents += jobdata(ma);
        }
        writeFile(taskId, contents);
        return taskId;
    }

    static String jobdata(MediaAsset ma) {
        if (ma == null) return "# null MediaAsset passed\n";
        Shepherd myShepherd = new Shepherd("context0");
        //i guess technically we only need encounter to get individual... which maybe we dont need?
        Encounter enc = null;
        for (Annotation ann : ma.getAnnotations()) {
            enc = Encounter.findByAnnotation(ann, myShepherd);
            if (enc != null) break;
        }
        if (enc == null) return "#unable to find Encounter for " + ma.toString() + "; skipping\n";
        //yup, this assumes LocalAssetStore, but thats our magic here
	String filePathString = ma.localPath().toString();
        String pathReplaceRegex = CommonConfiguration.getProperty("BenWhitesharkMediaAssetPathReplaceRegex", "context0");
        String pathReplaceValue = CommonConfiguration.getProperty("BenWhitesharkMediaAssetPathReplaceValue", "context0");
	if ((pathReplaceRegex != null) && (pathReplaceValue != null)) {
		filePathString = filePathString.replace(pathReplaceRegex, pathReplaceValue);
	}
        return ma.getUUID() + "\t" + filePathString + "\t" + (enc.hasMarkedIndividual() ? enc.getIndividualID() : "-1") + "\t" + enc.getCatalogNumber() +
            "\t-1\t-1\t-1\t-1\n";    // this is holding the place of the potential two fin end points x1,y1 x2,y2 (via user input)
    }

    static void writeFile(String taskId, String contents) {
        File dir = getJobStartDir();
        if (dir == null) throw new RuntimeException("no defined BenWhitesharkJobStartDirectory");
        File ftmp = new File(dir, taskId + ".tmp");  //dissuade race condition of reading before done
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(ftmp));
            writer.write(contents);
            writer.close();
            ftmp.renameTo(new File(dir, taskId));
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }


/*  results files look like:
ids	scores
59	0.242
64	0.043
50	0.039
48	0.03
40	0.024
15	0.022
1	0.02
89	0.018
*/
    //null means we dont have any
    // results are keyed off of MediaAsset id, which points to a LinkedHashMap of IndividualID/scores (from file)
    // if a key __ERROR__ exists, there was an error from the IA algorithm, which take the form of .err files, with the filename prefix
    // being either (a) the MediaAsset id [for specific error], or (b) taskId [for general]... notably this special key can exist in either map level
    public static HashMap<String,LinkedHashMap<String,Object>> getJobResultsRaw(String taskId) {
        File dir = new File(getJobResultsDir(), taskId);
        if (!dir.exists()) return null;
        HashMap<String,LinkedHashMap<String,Object>> rtn = new HashMap<String,LinkedHashMap<String,Object>>();
        //first lets check for a general error
        File gerr = new File(dir, taskId + ".err");
        if (gerr.exists()) {
            //we need to create a "results" LinkedHashMap to point to, so this is a little wonky
            LinkedHashMap<String,Object> m = new LinkedHashMap<String,Object>();
            m.put(ERROR_KEY, StringUtils.join(Util.readAllLines(gerr), ""));
            rtn.put(ERROR_KEY, m);  //thus, to find error msg for general case, need to use ERROR_KEY twice
            return rtn;  //is our work really done here?  seems like implies fail
        }
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) continue;  //"should never happen"
            LinkedHashMap<String,Object> m = new LinkedHashMap<String,Object>();
            String id = f.getName();  //will get truncated
            if (id.indexOf(".err") > -1) {
                id = id.substring(0, id.length() - 4);
                m.put(ERROR_KEY, StringUtils.join(Util.readAllLines(f), ""));
                rtn.put(id, m);
            } else if (id.indexOf(".txt") > -1) {
                id = id.substring(0, id.length() - 4);
                for (String line : Util.readAllLines(f)) {
                    if (line.indexOf("ids\t") == 0) continue;  //skip header
                    String[] data = StringUtils.split(line, "\t");
                    if (data.length < 2) continue;  //no?
                    Double score = -1.0;  //if parsing fails?
                    try {
                        score = Double.parseDouble(data[1]);
                    } catch (NumberFormatException ex) { };  //meh
                    m.put(data[0], score);
                }
                rtn.put(id, m);
            }
        }
        return rtn;
    }

    //grabs from ident log if we have it, otherwise it will attempt to grab raw job results
    public static JSONObject getTaskResults(String taskId, Shepherd myShepherd) {
        ArrayList<IdentityServiceLog> logs = IdentityServiceLog.loadByTaskID(taskId, SERVICE_NAME, myShepherd);
        if ((logs != null) && (logs.size() > 0)) {
            for (IdentityServiceLog log : logs) {
                if ((log.getStatusJson() != null) && (log.getStatusJson().optJSONObject("results") != null))
                    return log.getStatusJson().getJSONObject("results");
            }
        }
        System.out.println("NOTE: getTaskResults(" + taskId + ") fell thru, trying getJobResultsRaw()");
        HashMap<String,LinkedHashMap<String,Object>> raw = getJobResultsRaw(taskId);
        if (raw == null) return null;
        String[] ids = null;
        if (!raw.containsKey(ERROR_KEY)) {
            ids = new String[raw.keySet().size()];
            ids = raw.keySet().toArray(ids);
        }
        JSONObject jlog = new JSONObject();
        jlog.put("results", resultsAsJSONObject(raw));
        IdentityServiceLog log = new IdentityServiceLog(taskId, ids, SERVICE_NAME, null, jlog);
        log.save(myShepherd);
        return resultsAsJSONObject(getJobResultsRaw(taskId));
    }

    public static JSONObject resultsAsJSONObject(HashMap<String,LinkedHashMap<String,Object>> resMap) {
        if (resMap == null) return null;
        JSONObject matches = new JSONObject();
        if (resMap.containsKey(ERROR_KEY)) {
            matches.put("error", resMap.get(ERROR_KEY).get(ERROR_KEY));  //yes, we assume this will exist... cuz we set it as such
            return matches;
        }
        for (String mid : resMap.keySet()) {
            if (resMap.get(mid).containsKey(ERROR_KEY)) {
                matches.put(mid, resMap.get(mid).get(ERROR_KEY));
                continue;
            }
            JSONArray marr = new JSONArray();
            for (String iid : resMap.get(mid).keySet()) {
                JSONObject jscore = new JSONObject();
                jscore.put(iid, resMap.get(mid).get(iid));
                marr.put(jscore);
            }
            matches.put(mid, marr);
        }
        return matches;
    }

    public static JSONObject iaGateway(JSONObject arg, HttpServletRequest request) {
        JSONObject res = new JSONObject("{\"success\": false, \"error\": \"unknown\"}");
        String context = ServletUtilities.getContext(request);
        Shepherd myShepherd = new Shepherd(context);
        if (arg.optInt("identify", -1) > 0) {  //right now, start identify with {identify: maId}
            int mid = arg.getInt("identify");
            MediaAsset ma = MediaAssetFactory.load(mid, myShepherd);
            if (ma == null) {
                res.put("error", "unknown MediaAsset id=" + mid);
                return res;
            }
            String taskId = startJob(ma, myShepherd);
            res.put("success", true);
            res.remove("error");
            res.put("taskId", taskId);

        } else if (arg.optString("taskResults", null) != null) {
            String taskId = arg.getString("taskResults");
            res.put("taskId", taskId);
            JSONObject tres = getTaskResults(taskId, myShepherd);
            if (tres == null) {
                res.put("error", "no results for taskId=" + taskId);
                return res;
            }
            if (tres.opt("error") != null) {  //general failure!
                res.put("error", tres.get("error"));
                return res;
            }
            res.put("success", true);  //well, at least partially?
            res.remove("error");
            res.put("matches", tres);

        } else {
            res.put("error", "unknown command");
        }
        return res;
    }


}
