'use strict';

//app.configPromise.then(function() {
//    alert(JSON.stringify(app));
//});

var submitMedia = (function () {
    var markers = null;
    var hasGPS = false;
    var map = null;

    $(function() {
        map = maptool.createMap('map-mediasubmission');
    });

    function addToMap(latitude, longitude) {
        if (markers) {
            markers.forEach(function(marker) {
                map.map.removeLayer(marker);
            });
        }

        if (latitude && longitude) {
            markers = map.addIndividuals([[latitude, longitude]]);
//            var latlng = L.latLng(latitude, longitude);
//            marker = L.marker(latlng).addTo(map.map);
//            map.map.setView(latlng, 2);
        }

        //
        // Fixes problem with bootstrap keeping the map div
        // from properly auto-sizing.
        //
//        setTimeout(function () {
//            map.invalidateSize();
//        }, 1000);
    }

    //
    // With the datetimepicker add-on the defaultDate: null option doesn't work
    // and I can't for the life of me figure it out. What happens is that it instead
    // defaults to the current date with 00:00 time. So I just check if that is
    // what the string is I then assume that the user did not enter a date.  If
    // someone does choose the current date at midnight then this will be a problem.
    // Let's hope they don't?
    //
    function toTime(theDate) {
        if (! theDate) {
            return null;
        }

        if (theDate instanceof Date) {
            return theDate.getTime();
        }

        //
        // Otherwise assume string.
        //
        // First check to see that this isn't the "null" date defined above.
        //no
        var date = new Date();
        var year = date.getFullYear();
        var month = (1 + date.getMonth()).toString();
        month = month.length > 1 ? month : '0' + month;
        var day = date.getDate().toString();
        day = day.length > 1 ? day : '0' + day;
        if (year + '-' + month + '-' + day + ' 00:00' !== theDate) {
            return null;
        }

        //
        // WARNING: Firefox barfs when creating a date from "2012-10-25 08:57"!
        //          But, changing dashes to slashes seems to work for both Chrome
        //          and Firefox. There have been some suggestions of spaces instead
        //          which I will try if we find some browser not working with slashes.
        //
        return new Date(theDate.replace(/-/g, '/')).getTime();
    }

    var wizard = angular.module('nodeApp',
                                ['nodeApp.controllers',
                                 'rcWizard',
                                 'rcForm',
                                 'rcDisabledBootstrap',
                                 'ui.date',
                                 'blueimp.fileupload']);
//    .factory('dataService', function() {
//        var _data = {};
//        return {
//            data: _data
//        };
//    });

    wizard.controller('MediaSubmissionController',
//        function ($scope, $q, $timeout, dataService) {
//            $scope.data = dataService.data;
        function ($scope) {
//            $scope.$watch('data.config', function() {
////                alert("data\n" + JSON.stringify($scope.data.config));
//                if ($scope.data && $scope.data.config) {
//                    wizard.value("config", $scope.data.config);
//                }
//            });

            angular.element(document).ready(function() {
                map.map.on('click', function(event) {
                    $scope.$apply(function() {
                        if (! hasGPS) {
                            $scope.media.latitude = event.latlng.lat;
                            $scope.media.longitude = event.latlng.lng;
                        }
                    });
                });
            });

            function urlParam(name) {
                var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
                if (results==null){
                   return null;
                }
                else{
                   return results[1] || 0;
                }
            }

            //
            // NOTE: This is within an apply already so no need to wrap it in one.
            // In fact we CAN'T because it throws an error since it is already
            // in an apply. Can't run an apply within an apply.
            //
            $scope.media = {
                "submissionid": urlParam('submissionid'),
                "email": urlParam('email'),
                "name": urlParam('name'),
                "endTime": null,
                "startTime": null
            };

            //
            // Default the user to agreeing to the terms.
            //
            $scope.data = {agreeTerms: true};

            app.configPromise.done(function(){
                $scope.media.username = (app.user) ? app.user.username : null;
            });

            function longToDate(date) {
                if (date) {
                    return new Date(date);
                }

                return null;
            }

            function savems(media, method) {
                //
                // Don't alter the object directly because it causes
                // a conflict between the date string of the control and
                // the long we use here.
                //
                var ms = $.extend({}, media);

                var endTime = toTime(ms.endTime);
                if (isNaN(endTime)) {
                    alertplus.alert("End Time [" + ms.endTime + "] is not a valid time.");
                    return $.Deferred().reject();
                }
                ms.endTime = endTime;

                var startTime = toTime(ms.startTime);
                if (isNaN(startTime)) {
                    alertplus.alert("Start Time [" + ms.endTime + "] is not a valid time.");
                    return $.Deferred().reject();
                }
                ms.startTime = startTime;

                $(document.body).css({ 'cursor': 'wait' });
//                return $.post("wildbook/obj/mediasubmission/" + method, ms)
//                return $.post($scope.data.config.wildbook.proxyUrl + "/obj/mediasubmission/" + method, ms)
                return $.post(app.config.wildbook.proxyUrl + "/obj/mediasubmission/" + method, ms)
                .done(function(ex) {
                    $(document.body).css({ 'cursor': 'default' });
                })
                 .fail(function(ex) {
                     $(document.body).css({ 'cursor': 'default' });
                     alertplus.error(ex);
                 });
            }

            //
            // showTime = true is my addition to angular-ui/ui-date
            // which allows it to use the timepicker. See the mymaster branch.
            //
            $scope.dateOptions = {
                changeMonth: true,
                changeYear: true,
                dateFormat: 'yy-mm-dd',
    //                dateFormat: '@',
                showTime: true,
                defaultDate: null
            };

            $scope.isLoggedIn = function() {
//                return ($scope.data.user);
                return (app.user);
            };

            $scope.getExifData = function() {
                if (!this.queue.length) {
                    alertplus.alert("Please upload some media files before continuing.");
                    return $.Deferred().reject();
                }
                //
                // Loop through files and make sure they are all uploaded before
                // continuing.
                //
                var allUploaded = true;
                $.each(this.queue, function() {
                    //
                    // If our object in the queue does not have
                    // thumbnailUrl property set on it then it did not
                    // come from the server. So this is a janky, but I hope effective,
                    // way of knowing whether or not all files have been uploaded.
                    //
                    if (this.thumbnailUrl === undefined) {
                        allUploaded = false;
                    }
                });

                if (!allUploaded) {
                    alertplus.alert("Please finish uploading (or canceling) all of your images before continuing.");
                    return $.Deferred().reject();
                }

                if (!$scope.data.agreeTerms) {
                    alertplus.alert("Please agree to the terms and conditions before continuing");
                    return $.Deferred().reject();
                }

                //
                // Make call to get xif data
                //
//                var jqXHR = $.get("wildbook/obj/mediasubmission/getexif/" + $scope.media.id)
//                var jqXHR = $.get($scope.data.config.wildbook.proxyUrl + "/obj/mediasubmission/getexif/" + $scope.media.id)
                var jqXHR = $.get(app.config.wildbook.proxyUrl + "/obj/mediasubmission/getexif/" + $scope.media.id)
                .done(function(data) {
                    var avg = data.avg;
                    //
                    // If we have lat/long data then we don't want to show
                    // the lat/long data but rather just the stuff on the map.
                    //
                    if (avg.latitude && avg.longitude) {
                        hasGPS = true;
                        //
                        // Hide lat/long boxes if we have exif data.
                        //
                        $("#mediasubmissionlatlong").addClass("hidden");

                        //
                        // Add markers to map.
                        //
                        $.each(data.items, function() {
                            if (this.latitude && this.longitude) {
                                L.marker(L.latLng(this.latitude, this.longitude)).addTo(map.map);
                            }
                        });

                        map.map.setView(L.latLng(avg.latitude, avg.longitude), 2);
                    } else {
                        map.map.setView([0,0], 1);
                    }

                    //
                    // Fixes problem with bootstrap keeping the map div
                    // from properly auto-sizing.
                    //
                    setTimeout(function () {
                        map.map.invalidateSize();
                    }, 1000);


                    $scope.$apply(function() {
                        $scope.media.startTime = longToDate(avg.minTime);
                        $scope.media.endTime = longToDate(avg.maxTime);
                        $scope.media.latitude = avg.latitude;
                        $scope.media.longitude = avg.longitude;
                    });
                })
                .fail(function(ex) {
                    alertplus.error(ex);
                });

                return jqXHR.promise();
            };

            $scope.saveSubmission = function() {
                var jqXHR = savems($scope.media, "save")
                .done(function(data) {
console.log('data %o', data);
                    var mediaid = data.id;
                    $scope.$apply(function(){
                        //
                        // NOTE: This is bound using ng-value instead of ng-model
                        // because it is a hidden element.
                        //
                        $scope.media.id = mediaid;
                    });
                });

                return jqXHR.promise();
            };

            $scope.completeWizard = function() {
                var jqXHR = savems(this.media, "complete")
                .done(function(mediaid) {
                    $("#MediaSubmissionWizard").addClass("hidden");
                    $("#MediaSubmissionThankYou").removeClass("hidden");
                });

                return jqXHR.promise();
            };

    //                //
    //                // We need these watch expressions to force the changes to the model
    //                // to be reflected in the view.
    //                //
    //                $scope.$watch("media.startTime", function(newValue, oldValue) {
    //                    console.log("startTime changed from [" + oldValue + "] to [" + newValue + "]");
    //                    console.log(new Error().stack);
    //                });
            $scope.$watch("media.latitude", function(newVal, oldVal) {
                if ($scope.media.longitude) {
                    addToMap(newVal, $scope.media.longitude);
                }
            });
            $scope.$watch("media.longitude", function(newVal, oldVal) {
                if ($scope.media.latitude) {
                    addToMap($scope.media.latitude, newVal);
                }
            });
        }
    );

    wizard.config(['$httpProvider',
         'fileUploadProvider',
         function ($httpProvider, fileUploadProvider) {
            delete $httpProvider.defaults.headers.common['X-Requested-With'];
            fileUploadProvider.defaults.redirect = window.location.href.replace(
                    /\/[^\/]*$/,
                    '/cors/result.html?%s'
            );
            // Demo settings:
            angular.extend(fileUploadProvider.defaults, {
                // Enable image resizing, except for Android and Opera,
                // which actually support image resizing, but fail to
                // send Blob objects via XHR requests:
                disableImageResize: /Android(?!.*Chrome)|Opera/
                    .test(window.navigator.userAgent),
                maxFileSize: 500000000,
                acceptFileTypes: /(\.|\/)(gif|jpe?g|png|kmz|kml|gpx|avi|mpg|mp4|mov|wmv|flv)$/i
                //
                // This does work to control the area in which files are allowed
                // to be dropped to land in the control, but then if you drop it outside
                // the control Chrome things you want to view the images and opens them
                // thus taking you away from the wizard. Hitting the back button restarts
                // the wizard. Probably could figure out how to fix that...
                //
//                dropZone: $("#fileupload .dropbox")
            });
         }
    ])
    .controller('SubmissionFileUploadController',
        ['$scope', '$http',
         function ($scope, $http) {
            app.configPromise.done(function() {
                 $scope.options = {
    //                   url: "http://wildbook.happywhale.com/mediaupload"
    //                url: config.wildbook.url + "/mediaupload"
                    url: app.config.wildbook.url + "/mediaupload"
                 };
            });
//            $scope.loadingFiles = true;
// //            $http.get($scope.data.config.wildbook.url + "/mediaupload")
//            $http.get(app.config.wildbook.url + "/mediaupload")
//            .then(function (response) {
//                    $scope.loadingFiles = false;
//                    $scope.queue = response.data.files || [];
//                },
//                function () {
//                    $scope.loadingFiles = false;
//                }
//            );

            $scope.loadingFiles = false;
            $scope.queue = [];
        }
    ])
    .controller('FileDestroyController',
        ['$scope', '$http',
//         function ($scope, $http, dataService) {
//            $scope.data = dataService.data;
         function ($scope, $http) {
            var file = $scope.file;
            if (file.url) {
                file.$destroy = function () {
                    return $http({
//                        url: "wildbook/obj/mediasubmission/delfile/" + $scope.media.id,
//                      url: $scope.data.config.wildbook.proxyUrl + "/obj/mediasubmission/delfile/" + $scope.media.id,
                        url: app.config.wildbook.proxyUrl + "/obj/mediasubmission/delfile/" + $scope.media.id,
                        data: this.name,
                        method: 'POST'
                    })
                    .then(function () {
                            $scope.clear(file);
                        },
                        function () {
                            console.log("failed to delete");
                        }
                    );
                };
            } else if (!file.$cancel && !file._index) {
                file.$cancel = function () {
                    $scope.clear(file);
                };
            }
        }
    ]);

//
//    return {"log": function() {
//        console.log(JSON.stringify(smms.media));
//    }};
})();
