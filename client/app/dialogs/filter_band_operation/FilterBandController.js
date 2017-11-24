/**
 * Created by a.corrado on 24/05/2017.
 */


var FilterBandController = (function() {

    function FilterBandController($scope, oClose,oExtras,oWorkspaceService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oExtras = oExtras;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_aoWorkspaceList = [];
        this.m_oClose = oClose;
        this.m_iActiveProvidersTab = 0;
        this.m_iSelectedValue = 0 ;
        this.m_aoFilterProperties = [
            {name:"Operation:",value:""},
            {name:"Name:",value:""},
            {name:"Shorthand:",value:""},
            {name:"Tags:",value:""},
            {name:"Kernel quotient:",value:""},
            {name:"Kernel offset X:",value:""},
            {name:"Kernel offset Y:",value:""},
            {name:"Kernel width:",value:""},
            {name:"Kernel height:",value:""},
        ];
        this.m_aiFillOptions=[-2,-1,0,1,2,3,4,5];
        this.testMatrix=[
            [
                {color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},
                {color:"red" ,fontcolor:"black", value:"0" , click:function(){console.log('hello')}},
                {color:"yellow" ,fontcolor:"black", value:"0" , click:function(){console.log('hello')}},
                {color:"yellow" ,fontcolor:"black", value:"0"}
            ],
            [
                {color:"white",fontcolor:"black",value:"0" , click:function(){console.log('hello')}},
                {color:"red" ,fontcolor:"black", value:"1"},
                {color:"black" , fontcolor:"white",value:"0"},
                {color:"yellow" ,fontcolor:"black", value:"0"}
            ],
            [{color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},{color:"red" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"}],
            [{color:"blue" ,fontcolor:"black", value:"1", click:function(){console.log('hello')}},{color:"red" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"},{color:"yellow" ,fontcolor:"black", value:"0"}],

        ];
        this.m_aoSystemFilterOptions = [
            {name:"Detect Lines" ,options:[
                    {name:"Horizontal Edges",actions:function(){}},
                    {name:"Vertical Edges",actions:function(){}},
                    {name:"Left Diagonal Edges",actions:function(){}},
                    {name:"Compass Edge Detector",actions:function(){}},
                    {name:"Diagonal Compass Edges Detector",actions:function(){}},
                    {name:"Roberts Cross North-West",actions:function(){}},
                    {name:"Roberts Cross North-East",actions:function(){}},

                ]},
            {name:"Detect Gradients" ,options:[
                    {name:"Sobel North",actions:function(){}},
                    {name:"Sobel South",actions:function(){}},
                    {name:"Sobel West",actions:function(){}},
                    {name:"Sobel East",actions:function(){}},
                    {name:"Sobel North East",actions:function(){}},
                ]},
            {name:"Smooth and Blurr" ,options:[
                {name:"Arithmetic Mean 3x3",actions:function(){}},
                {name:"Arithmetic Mean 4x4",actions:function(){}},
                {name:"Arithmetic Mean 5x5",actions:function(){}},
                {name:"Low-Pass 3x3",actions:function(){}},
                {name:"Low-Pass 5x5",actions:function(){}},


            ]},
            {name:"Sharpen" ,options:[
                {name:"High-Pass 3x3 #1",actions:function(){}},
                {name:"High-Pass 3x3 #2",actions:function(){}},
                {name:"High-Pass 3x3 5x5",actions:function(){}},

            ]},
            {name:"Enhance Discontinuities" ,options:[
                {name:"Laplace 3x3(a)",actions:function(){}},
                {name:"Laplace 3x3(b)",actions:function(){}},
                {name:"Laplace 5x5(a)",actions:function(){}},
                {name:"Laplace 5x5(b)",actions:function(){}},
            ]},
            {name:"Non-linear Filters" ,options:[
                {name:"Minimum 3x3",actions:function(){}},
                {name:"Minimum 5x5",actions:function(){}},
                {name:"Minimum 7x7",actions:function(){}},
                {name:"Maximum 3x3",actions:function(){}},
                {name:"Maximum 5x5",actions:function(){}},
                {name:"Maximum 7x7",actions:function(){}},
                {name:"Mean 3x3",actions:function(){}},
                {name:"Mean 5x5",actions:function(){}},
                {name:"Mean 7x7",actions:function(){}},
                {name:"Median 3x3",actions:function(){}},
                {name:"Median 5x5",actions:function(){}},
                {name:"Median 7x7",actions:function(){}},
                {name:"Standard Deviation 3x3",actions:function(){}},
                {name:"Standard Deviation 5x5",actions:function(){}},
                {name:"Standard Deviation 7x7",actions:function(){}},
            ]},
            {name:"Morphological Filters" ,options:[
                {name:"Erosion 3x3",actions:function(){}},
                {name:"Erosion 5x5",actions:function(){}},
                {name:"Erosion 7x7",actions:function(){}},
                {name:"Dilation 3x3",actions:function(){}},
                {name:"Dilation 5x5",actions:function(){}},
                {name:"Dilation 7x7",actions:function(){}},
                {name:"Opening 3x3",actions:function(){}},
                {name:"Opening 5x5",actions:function(){}},
                {name:"Opening 7x7",actions:function(){}},
                {name:"Closing 3x3",actions:function(){}},
                {name:"Closing 5x5",actions:function(){}},
                {name:"Closing 7x7",actions:function(){}},
            ]},
        ];

        this.m_aoUserFilterOptions = [
            {name:"User" ,options:[]},
        ];

        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 200); // close, but give 500ms for bootstrap to animate
        };

    }
    FilterBandController.prototype.closeAndDonwloadProduct= function(result){

        this.m_oClose(result, 500); // close, but give 500ms for bootstrap to animate

    };

    FilterBandController.prototype.addUserFilterOptions = function()
    {
        var oThat = this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}//
        };
        var aaoEmptyMatrix = this.makeEmptyMatrix(4,4,oDefaultValue);
        this.m_aoUserFilterOptions[0].options.push( {name:"NewFilter", matrix:aaoEmptyMatrix});
    };

    /*TODO REMOVE*/
    FilterBandController.prototype.removeUserFilterOptions = function(){
    };

    FilterBandController.prototype.makeEmptyMatrix = function(iNumberOfCollumns,iNumberOfRows,iDefaultValue){
        if( utilsIsObjectNullOrUndefined(iNumberOfCollumns) || utilsIsObjectNullOrUndefined(iNumberOfRows)|| utilsIsObjectNullOrUndefined(iDefaultValue) )
            return null;
        var aaMatrix = [];
        for(iIndexRows = 0; iIndexRows <  iNumberOfRows; iIndexRows++)
        {

            var aoRow = [];
            for(iIndexCollumns = 0; iIndexCollumns <  iNumberOfCollumns; iIndexCollumns++)
            {
               //aoRow.push(iDefaultValue);
               aoRow[iIndexCollumns] =  {color: iDefaultValue.color ,fontcolor:iDefaultValue.fontcolor, value:iDefaultValue.value, click:iDefaultValue.click};
            }

            aaMatrix[iIndexRows] = aoRow;

        }

        return aaMatrix;
    };
    FilterBandController.prototype.numberOfRows = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix))
            return 0;
        return aaoMatrix.length;
    };

    FilterBandController.prototype.numberOfCollumns = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return 0;
        return aaoMatrix[0].length;
    };


    FilterBandController.prototype.addRowInMatrix = function(aaoMatrix,oElementRow)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || utilsIsObjectNullOrUndefined(oElementRow)  )
            return false;

        if(aaoMatrix.length === 0)
            var iNumberOfRowValues = 5;//default value
        else
            var iNumberOfRowValues = aaoMatrix[0].length;

        var aoRow = [];
        for(var iIndexRow = 0; iIndexRow < iNumberOfRowValues; iIndexRow++)
        {
            aoRow[iIndexRow]=({
                color:oElementRow.color ,
                fontcolor:oElementRow.fontcolor,
                value:oElementRow.value,
                click:oElementRow.click
            });

        }

        aaoMatrix.push(aoRow);
        return true;
    };

    FilterBandController.prototype.addDefaultRowInMatrix = function(aaoMatrix)
    {
        var oThat=this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}
        };
        return this.addRowInMatrix(aaoMatrix,oDefaultValue)
    };

    FilterBandController.prototype.removeRowInMatrix = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || aaoMatrix.length <= 0)
            return false;
        aaoMatrix.pop();
        return true;
    };

    FilterBandController.prototype.addColumnInMatrix = function(aaoMatrix,oDefaultValue)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix) || utilsIsObjectNullOrUndefined(oDefaultValue) )
            return false;

        var iNumberOfRows = aaoMatrix.length;

        for(var iIndexRows=0; iIndexRows < iNumberOfRows; iIndexRows++)
        {
            //var iIndexNumberOf = aaoMatrix[iIndexRows].length;
            aaoMatrix[iIndexRows].push({color: oDefaultValue.color ,fontcolor:oDefaultValue.fontcolor, value:oDefaultValue.value, click:oDefaultValue.click});
        }



        return true;
    };
    FilterBandController.prototype.addDefaultColumnInMatrix = function(aaoMatrix)
    {
        var oThat=this;
        var oDefaultValue = {
            color:"white" ,
            fontcolor:"black",
            value:"0",
            click:function(){this.value = oThat.m_iSelectedValue;}
        };
        return this.addColumnInMatrix(aaoMatrix,oDefaultValue)
    };
    FilterBandController.prototype.removeColumnInMatrix = function(aaoMatrix)
    {
        if(utilsIsObjectNullOrUndefined(aaoMatrix))
            return false;
        var iNumberOfRows = aaoMatrix.length;
        for(var iIndexRows=0; iIndexRows < iNumberOfRows; iIndexRows++)
        {
            //var iIndexNumberOf = aaoMatrix[iIndexRows].length;
            aaoMatrix[iIndexRows].pop();
        }
        return true;
    };

    FilterBandController.$inject = [
        '$scope',
        'close',
        'extras',
        'WorkspaceService'
    ];
    return FilterBandController;
})();
