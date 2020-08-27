function ViewElementFactory() {

    /**
     * Creates a single view element from a control in input.
     * @param oControl Control Description: minimum properties are param, type, label
     * @returns {TextBox|SelectArea|ProductList|CheckBox|SearchEOImage|DateTimePicker|*}
     */
    this.createViewElement = function (oControl) {

        // Return variable
        let oViewElement;

        // Find the right type and create the element
        if (oControl.type === "textbox") {

            // Text box
            oViewElement = new TextBox();

            // See if we have a default
            if (oControl.default) {
                oViewElement.m_sText = oControl.default;
            }
        }
        else if (oControl.type === "dropdown") {
            // Drop Down
            oViewElement = new DropDown();
        }
        else if (oControl.type === "bbox") {
            // Bounding Box from Map
            oViewElement = new SelectArea();
        }
        else if (oControl.type === "date"){
            oViewElement = new DateTimePicker();
        }
        else if (oControl.type === "productlist"){
            oViewElement = new ProductList();
        }
        else if (oControl.type === "searcheoimage"){
            oViewElement = new SearchEOImage();
        }
        else if(oControl.type === "boolean"){
            oViewElement = new CheckBox();

            if (utilsIsObjectNullOrUndefined(oControl.default) == false) {
                oViewElement.m_bValue = oControl.default;
            }
        }
        else {
            oViewElement = new TextBox();
        }

        oViewElement.type = oControl.type;
        oViewElement.label = oControl.label;
        oViewElement.paramName = oControl.param;

        return oViewElement;
    }

    this.getTabElements = function (oTab) {

        let aoTabElements = [];

        for (let iControl=0; iControl<oTab.controls.length; iControl ++) {
            let oControl = oTab.controls[iControl];

            let oViewElement = this.createViewElement(oControl);

            aoTabElements.push(oViewElement);
        }

        return aoTabElements;
    }
}

/**
 * Search EO Image Control Class
 * @constructor
 */
let SearchEOImage = function() {
    this.oTableOfProducts = new ProductList();
    this.oStartDate = new DateTimePicker();
    this.oEndDate = new DateTimePicker();
    this.oSelectArea = new SelectArea();
    this.aoProviders = [];
    this.aoMissionsFilters = [];


    /*
    let tst7 = oFactory.CreateViewElement("searcheoimage");
    tst7.sLabel = "Sono una light search";
    tst7.oStartDate.m_sDate =  moment().subtract(1, 'days').startOf('day');
    tst7.oEndDate.m_sDate = moment();
    tst7.oSelectArea.iHeight = 200;
    tst7.oSelectArea.iWidth = 500;
    tst7.aoProviders.push(providers.ONDA);
    tst7.aoMissionsFilters.push({name:"sentinel-1" },{name:"sentinel-2" });
    tst7.oTableOfProducts.isAvailableSelection = true;
    tst7.oTableOfProducts.isSingleSelection = true;
    */

    /**
     *
     * @returns {string}
     */
    this.getValue = function () {
        return "";
    }
};

/**
 * Product List Control Class
 * @constructor
 */
let ProductList = function(){
    this.aoProducts = [];
    this.isAvailableSelection = false;
    this.isSingleSelection = true;
    this.oSingleSelectionLayer = {};

    /**
     *
     * @returns {{}}
     */
    this.getValue = function () {
        return this.oSingleSelectionLayer;
    }
};

/**
 * Date Time Picker Control Class
 * @constructor
 */
let DateTimePicker = function(){
    this.m_sDate = null;

    /**
     * Returns the selected Date
     * @returns {string|null} Date as a string in format YYYY-MM-DD
     */
    this.getValue = function () {
        if (this.m_sDate) {
            return this.m_sDate;
        }
        else {
            return "";
        }
    }
};

/**
 * Select Area (bbox) Control Class
 * @constructor
 */
let SelectArea = function () {
    this.oBoundingBox = {
        northEast : "",
        southWest : ""
    };
    this.iWidth = "";
    this.iHeight = "";

    /**
     * Return the bounding box as a string.
     * @returns {string} BBox as string: LATN,LONW,LATS,LONE
     */
    this.getValue = function () {
        try {
            if (this.oBoundingBox) {
                return "" + this.oBoundingBox.northEast.lat.toFixed(2) + "," + this.oBoundingBox.southWest.lng.toFixed(2)+"," + this.oBoundingBox.southWest.lat.toFixed(2)+","+ + this.oBoundingBox.northEast.lng.toFixed(2);
            }
            else {
                return "";
            }
        }
        catch (e) {
            return "";
        }


    }
};

/**
 * Text Box Control Class
 * @constructor
 */
let TextBox = function () {
    this.m_sText = "";

    /**
     * Get the value of the textbox
     * @returns {string} String in the textbox
     */
    this.getValue = function () {
        return this.m_sText;
    }
};

/**
 * Check box Control Class
 * @constructor
 */
let CheckBox = function () {
    this.m_bValue = true;

    /**
     * Return the value of the checkbox
     * @returns {boolean} True if selected, False if not
     */
    this.getValue = function () {
        return this.m_bValue;
        /*if () {
            return "1";
        }
        else {
            return "0";
        }*/
    }
};

/**
 * Drop Down Control Class
 * @constructor
 */
let DropDown = function () {
    this.asListValues = [];
    this.sSelectedValues = "";
    this.oOnClickFunction = null;
    this.bEnableSearchFilter = true;
    this.sDropdownName = "";

    /**
     * Get the selected value
     * @returns {string}
     */
    this.getValue = function () {
        return this.sSelectedValues;
    }
};