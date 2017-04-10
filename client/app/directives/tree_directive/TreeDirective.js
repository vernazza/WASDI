/**
 * Created by a.corrado on 22/11/2016.
 */
angular.module('wasdi.TreeDirective', [])
    .directive('tree', function () {
        "use strict";

        function linkFunction($scope, element, attr){

            var generateWellFormedTree;// method

            /* the oTreeAttribute is a object
             * as in the following:
             * {
             *    'text' : 'Name',
             *    'Children':[] <----- {
             *                              'text':'name'
             *                               'Children':[]
             *                         }
             * }
             * */

            //this.generateWellFormedTree=function(oElement,oNewTree,iIndexNewTreeAttribute)
            //{
            //
            //
            //    if (typeof oElement != "undefined" && oElement != null)
            //    {
            //        /* i generate new object
            //         {
            //         *       'text':'name'
            //         *       'Children':[]
            //         * }
            //         * */
            //
            //        var oNode = new Object();
            //        oNode.text=oElement.name;
            //        oNode.children= [];
            //        oNewTree.push(oNode);
            //
            //        if(oElement.elements != null)// if is a leaf
            //        {
            //            // i call the algorithm for all child
            //            for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
            //            {
            //                this.generateWellFormedTree(oElement.elements[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
            //            }
            //        }
            //
            //        /*
            //         if(oElement.bands != null)// if is a leaf
            //         {
            //         // i call the algorithm for all child
            //         for (var iIndexNumberElements = 0; iIndexNumberElements < (oElement.elements.length); iIndexNumberElements++)
            //         {
            //         this.generateWellFormedTree(oElement.bands[iIndexNumberElements] ,oNewTree[iIndexNewTreeAttribute].children, iIndexNumberElements);
            //         }
            //         }*/
            //    }
            //
            //}


            /*
            *  NOTE: $SCOPE = EDITOR SCOPE
            * */

            /* WHEN THE TREE WAS CHANGED */
            $scope.$watch('m_oController.m_oTree', function (newValue, oldValue, scope)
            {
                if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_oTree))
                {
                    if(!utilsIsObjectNullOrUndefined(oldValue))
                    {
                        /******************** RELOADED TREE CASE ****************************/
                        //if the tree is reloaded need  $('#jstree').jstree(true).refresh();
                        $('#jstree').jstree(true).settings.core = newValue.core;
                        $('#jstree').jstree(true).refresh();
                        $scope.m_oController.selectNodeByFileNameInTree($scope.m_oController.m_oLastDownloadedProduct);

                    }
                    else
                    {
                        //****************** INIT JS TREE  *************************
                        //load tree
                        $('#jstree').jstree($scope.m_oController.m_oTree);

                        //Bind Click node event
                        $('#jstree').on("changed.jstree", function (e, data) {

                            /*CLICK ON PUBLIC BAND*/
                            // if there isn't running publish band processes AND the node it's a band do $scope.m_oController.openBandImage()
                            //change icons
                            //data.event.type !="contextmenu" => discard right click of mouse (plugin)
                            if (angular.isUndefined(data.event))
                                return;
                            if(!utilsIsObjectNullOrUndefined(data.node) && data.event.type !="contextmenu")
                            {

                                if($scope.m_oController.m_oProcessesLaunchedService.thereIsPublishBandProcessOfTheProduct(data.node.id) == false && data.node.children.length == 0 && !utilsIsObjectNullOrUndefined(data.node.original.band))
                                {

                                    //if(data.node.icon == 'assets/icons/check.png')
                                    if(data.node.original.bPubblish == true)
                                    {
                                        data.node.original.bPubblish = false;
                                        //$('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/uncheck.png');
                                        $('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/uncheck_20x20.png');
                                        $scope.m_oController.removeBandImage(data.node.original.band);
                                    }
                                    else
                                    {
                                        //the tree icon is change when it receive the "publishband" message by rabbit or
                                        //when the band was pubblished (http request)
                                        // method: receivedPublishBandMessage()
                                        //data.node.original.bPubblish = true;
                                        //$('#jstree').jstree(true).set_icon(data.node.id, 'assets/icons/check.png');
                                        $scope.m_oController.openBandImage(data.node.original.band,data.node.id);
                                    }

                                }
                            }
                        });

                        ///* BIND EVENT DOUBLE CLICK */
                        //$("#jstree").delegate("a","dblclick", function(e) {
                        //    var instance = $.jstree.reference(this);
                        //    var node = instance.get_node(this);
                        //
                        //});

                        //bind event (event = after tree is loaded do checkTreeNode())
                        /*
                         * */
                        // $('#jstree').on("loaded.jstree", function (e, data)
                        // {
                        //    //if the page was reload it method check all nodes in tree,
                        //    //this nodes are processes running in server
                        //     $scope.m_oController.selectNodeByFileNameInTree($scope.m_oController.m_oLastDownloadedProduct);
                        // });

                    }
                }

            });


        }

        return{
            restrict:"E",

            template:'<div id="jstree"class="jstree" ></div>',
            link: linkFunction
        };
});