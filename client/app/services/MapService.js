/**
 * Created by p.campanella on 21/10/2016.
 */

'use strict';
angular.module('wasdi.MapService', ['wasdi.ConstantsService']).
service('MapService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    /**
     * base layers
     */

    this.initTileLayer= function(){
        this.m_oOSMBasic = L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 18
        });
        this.m_oOSMMapquest = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png', {
            subdomains: "12",
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.mapquest.com/" target="_blank">MapQuest</a> <img src="https://developer.mapquest.com/content/osm/mq_logo.png">',
            maxZoom: 18
        });
        this.m_oOSMHumanitarian = L.tileLayer('http://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://hot.openstreetmap.org/" target="_blank">Humanitarian OpenStreetMap Team</a>',
            maxZoom: 18
        });
        this.m_oOCMCycle = L.tileLayer('http://{s}.tile.opencyclemap.org/cycle/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.thunderforest.com/" target="_blank">Andy Allan</a>',
            maxZoom: 18
        });
        this.m_oOCMTransport = L.tileLayer('http://{s}.tile2.opencyclemap.org/transport/{z}/{x}/{y}.png', {
            attribution:
                '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors. Tiles courtesy of <a href="http://www.thunderforest.com/" target="_blank">Andy Allan</a>',
            maxZoom: 18
        });
    }

    //init tile layer
    this.initTileLayer();

    this.m_oGoogleHybrid = new L.gridLayer.googleMutant('hybrid');
    this.m_oGoogleMap = new L.gridLayer.googleMutant('roadmap');
    this.m_oGoogleTerrain = new L.gridLayer.googleMutant('terrain');
    this.m_oGoogleSatelite = new L.gridLayer.googleMutant('satellite');

    /**
     * layers control
     */
    this.m_oLayersControl = L.control.layers(
        {
            "Standard": this.m_oOSMBasic,
            "Cycle Map": this.m_oOCMCycle,
            "Transport Map": this.m_oOCMTransport,
            "Humanitarian": this.m_oOSMHumanitarian,
            "Google Hybrid": this.m_oGoogleHybrid,
            "Google Map": this.m_oGoogleMap,
            "Google Satellite": this.m_oGoogleSatelite,
            "Google Terrain": this.m_oGoogleTerrain
        },
        {},
        {
            'position' : 'bottomright'
        }
    );

    //MAP
    this.m_oWasdiMap = null;


    this.m_oActiveBaseLayer = this.m_oOSMBasic;

    this.clearMap = function () {
        if (this.m_oWasdiMap) {
            this.m_oWasdiMap.remove();
            this.m_oWasdiMap = null;
        }
    }


    this.initMap = function(sMapDiv) {


        /**
         * the map
         */
        if(this.m_oWasdiMap != null)
        {
            this.initTileLayer();
        }

        /*  it need disabled keyword, ther'is a bug :
        *   https://github.com/Leaflet/Leaflet/issues/1228
        *   thw window scroll vertically when i click (only if the browser window are smaller)
        *   alternative solution (hack):
        *   L.Map.addInitHook(function() {
        *   return L.DomEvent.off(this._container, "mousedown", this.keyboard._onMouseDown);
        *   });
        */
        this.m_oWasdiMap = L.map(sMapDiv, {
            zoomControl: false,
            layers: [this.m_oOSMBasic],
            keyboard: false,

            // maxZoom: 22
        });


        /**
         * scale control
         */
        L.control.scale({
            position: "bottomright",
            imperial: false
        }).addTo(this.m_oWasdiMap);


        /**
         * layers control
         */
        this.m_oLayersControl.addTo(this.m_oWasdiMap);

        /**
         * fitBounds
         */

        var southWest = L.latLng(40.712, -74.227),
            northEast = L.latLng(40.774, -74.125),
            oBoundaries = L.latLngBounds(southWest, northEast);

        this.m_oWasdiMap.fitBounds(oBoundaries);

        var oActiveBaseLayer = this.m_oActiveBaseLayer;

        //add event on base change
        this.m_oWasdiMap.on('baselayerchange', function(e){
            // console.log(e);
            e.layer.bringToBack()
            oActiveBaseLayer = e;
        });


        //add event on base change
        //this.m_oWasdiMap.on('load', function(e){
        //    oWasdiMap.invalidateSize();
        //});
    }

    this.initMapWithDrawSearch = function(sMapDiv)
    {
        //Init standard map
        this.initMap(sMapDiv);
        //LEAFLET.DRAW LIB
        //add draw.search
        var drawnItems = new L.FeatureGroup();
        this.m_oWasdiMap.addLayer(drawnItems);

        var oOptions={
            position:'topright',
            draw:{
                marker:false,
                polyline:false,
                circle:false
            },

            edit: {
                featureGroup: drawnItems,
            }
        };

        var oDrawControl = new L.Control.Draw(oOptions);

        this.m_oWasdiMap.addControl(oDrawControl);

        //Without this.m_oWasdiMap.on() the shape isn't saved on map
        this.m_oWasdiMap.on(L.Draw.Event.CREATED, function (event) {
            var layer = event.layer;

            //remove old shape
            if(drawnItems && drawnItems.getLayers().length!==0){
                drawnItems.clearLayers();
            }
            //save new shape in map
            drawnItems.addLayer(layer);
        });

        //TODO event EDITED
        //this.m_oWasdiMap.on(L.Draw.Event.EDITED, function (event) {
        //    var layer = event.layers;
        //});

    }

    this.getMap = function () {
        return this.m_oWasdiMap;
    }


}]);