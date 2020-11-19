/**
 * Created by a.corrado on 18/11/2016.
 */
var RootController = (function() {

    RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID = "RootController.openLogsDialogProcessId";

    function RootController($scope, oConstantsService, oAuthService, $state, oProcessesLaunchedService, oWorkspaceService,
                            $timeout,oModalService,oRabbitStompService, $window) {

        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=$state;
        this.m_oProcessesLaunchedService = oProcessesLaunchedService;
        this.m_oWorkspaceService = oWorkspaceService;
        this.m_oScope.m_oController=this;
        this.m_aoProcessesRunning=[];
        this.m_aoWorkspaces=[];
        this.m_iNumberOfProcesses = 0;
        this.m_iWaitingProcesses = 0;
        this.m_oLastProcesses = null;
        this.m_bIsOpenNav = false;
        this.m_bIsOpenStatusBar = false; //processes bar
        this.m_oModalService = oModalService;
        this.m_oRabbitStompService = oRabbitStompService;
        this.m_bIsEditModelWorkspaceNameActive = false;
        this.m_isRabbitConnected = true;
        this.m_oSummary = {};
        this.m_oWindow = $window;
        var oController = this;


        this.updateRabbitConnectionState = function(forceNotification)
        {
            if( forceNotification == null || forceNotification == undefined){
                forceNotification = false;
            }
            var connectionState = oRabbitStompService.getConnectionState();
            if( connectionState == 1) {
                oController.m_isRabbitConnected = true;
            }
            else
            {
                oController.m_isRabbitConnected = false;
                if( connectionState == 2)
                {
                    if(oRabbitStompService.m_oRabbitReconnectAttemptCount === 0 || forceNotification === true)
                    {
                        this.signalRabbitConnectionLost();
                    }
                }
            }
        };


        this.signalRabbitConnectionLost = function()
        {
            var dialog = utilsVexDialogAlertBottomRightCorner("Async server connection lost");
            utilsVexCloseDialogAfter(5000, dialog);
        };


        // Subscribe to 'rabbit service connection changes'
        var _this = this;

        var msgHlp = MessageHelper.getInstanceWithAnyScope($scope);

        msgHlp.subscribeToRabbitConnectionStateChange(function(event, args) {
            _this.updateRabbitConnectionState();
        });

        // then immediatly check rabbit connection state
        this.updateRabbitConnectionState(true);

        /**
         * Check user session
         */
        this.m_oAuthService.checkSession().success(function (data, status) {
            if (data === null || data === undefined || data === '' || data.userId === ''  )
            {

                oController.m_oConstantsService.logOut();
                oController.m_oState.go("home");
            }
            else
            {
                oController.m_oUser = oController.m_oConstantsService.getUser();
            }
        }).error(function (data,status) {
            utilsVexDialogAlertTop('ERROR IN CHECK ID SESSION');
            oController.onClickLogOut();
        });

        //if user is logged
        if(!utilsIsObjectNullOrUndefined(this.m_oConstantsService.getUser()))  this.m_oUser = this.m_oConstantsService.getUser();
        else this.m_oState.go("login");

        this.m_sWorkSpace = this.m_oConstantsService.getActiveWorkspace();

        if(utilsIsObjectNullOrUndefined(this.m_sWorkSpace) && utilsIsStrNullOrEmpty( this.m_sWorkSpace))
        {
            //if this.m_oState.params.workSpace in empty null or undefined create new workspace
            if(!(utilsIsObjectNullOrUndefined(this.m_oState.params.workSpace) && utilsIsStrNullOrEmpty(this.m_oState.params.workSpace)))
            {
                this.openWorkspace(this.m_oState.params.workSpace);
            }
            else
            {
                //TODO CREATE NEW WORKSPACE OR GO HOME
            }
        }

        this.m_aoProcessesRunning = this.m_oProcessesLaunchedService.getProcesses();

        /*when ProccesLaunchedservice reload the m_aoProcessesRunning rootController reload m_aoProcessesRunning */
        $scope.$on('m_aoProcessesRunning:updated', function(event,data) {
            // you could inspect the data to see
            if(data == true)
            {
                var aoProcessesRunning = $scope.m_oController.m_oProcessesLaunchedService.getProcesses();
                if(utilsIsObjectNullOrUndefined(aoProcessesRunning) == true) return;


                // Set the number of running processes

                $scope.m_oController.getSummary();

                $scope.m_oController.m_oLastProcesses = $scope.m_oController.findLastProcess(aoProcessesRunning);

                $scope.m_oController.m_aoProcessesRunning = $scope.m_oController.initializeTimeCounter(aoProcessesRunning);

            }

        });

        $scope.$on(RootController.BROADCAST_MSG_OPEN_LOGS_DIALOG_PROCESS_ID, function(event,data) {

            let intervalId = setInterval(function(){
                if( $scope.m_oController.openLogsDialogByProcessId(data.processId) == true){
                    clearInterval(intervalId);
                }
            }, 1000);
        });



        /* WATCH  ACTIVE WORKSPACE IN CONSTANT SERVICE
        * every time the workspace change, it clean the log list &&
        * set m_bIsEditModelWorkspaceNameActive = false
        * */
        $scope.$watch('m_oController.m_oConstantsService.m_oActiveWorkspace', function(newValue, oldValue, scope) {
            $scope.m_oController.m_aoProcessesRunning = [];
            $scope.m_oController.m_bIsEditModelWorkspaceNameActive = false;
            if(utilsIsObjectNullOrUndefined(newValue) === false)
            {
                if(newValue.name === "Untitled Workspace")
                {
                    $scope.m_oController.getWorkspacesInfo();
                    $scope.m_oController.editModelWorkspaceNameSetTrue();
                }
            }
        });


        /*COUNTDOWN METHOD*/

        //this.time = 0;

        $scope.onTimeout = function()
        {
            if(!utilsIsObjectNullOrUndefined($scope.m_oController.m_aoProcessesRunning) && $scope.m_oController.m_aoProcessesRunning.length != 0)
            {
                var iNumberOfProcesses = $scope.m_oController.m_aoProcessesRunning.length;

                for(var iIndexProcess = 0; iIndexProcess < iNumberOfProcesses;iIndexProcess++ )
                {
                    if ($scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="RUNNING" ||
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="WAITING" ||
                        $scope.m_oController.m_aoProcessesRunning[iIndexProcess].status==="READY") {
                         $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.setSeconds( $scope.m_oController.m_aoProcessesRunning[iIndexProcess].timeRunning.getSeconds() + 1) ;
                    }
                }
            }
            //$scope.m_oController.time++;

        };

        this.isProcesssBarOpened = function()
        {
            return this.m_bIsOpenStatusBar;
        };




        this.getWorkspacesInfo();
        this.initTooltips();
        this.getSummary()
    }

    /*********************************** METHODS **************************************/

    RootController.prototype.initializeTimeCounter = function(aoProcessesRunning)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessesRunning) === true )
        {
            return null;
        }
        var iTotalProcessesNumber = aoProcessesRunning.length;

        for( var  iIndexNewProcess= 0; iIndexNewProcess  < iTotalProcessesNumber; iIndexNewProcess++)
        {
            //time by server
            var oStartTime;
            var oNow;
            var result;
            var seconds;
            var oDate;
            if (aoProcessesRunning[iIndexNewProcess].status === "RUNNING" || aoProcessesRunning[iIndexNewProcess].status === "WAITING" || aoProcessesRunning[iIndexNewProcess].status === "READY")
            {
                if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                    // add start time (useful if the page was reloaded)

                    //time by server
                    oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationStartDate);
                    //pick time
                    oNow = new Date();
                    result =  Math.abs(oNow-oStartTime);
                    //approximate result
                    seconds = Math.ceil(result / 1000);

                    if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0 || utilsIsANumber(seconds)=== false)
                    {
                        seconds = 0;
                    }

                    oDate = new Date(1970, 0, 1);
                    oDate.setSeconds(0 + seconds);
                    //add running time
                    aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                }
            }
            else {
                if (utilsIsObjectNullOrUndefined(aoProcessesRunning[iIndexNewProcess].timeRunning)) {
                    aoProcessesRunning[iIndexNewProcess].timeRunning = 0;

                    //time by server
                    oStartTime = new Date(aoProcessesRunning[iIndexNewProcess].operationStartDate);
                    oEndTime = new Date(aoProcessesRunning[iIndexNewProcess].operationEndDate);
                    //pick time
                    result =  Math.abs(oEndTime-oStartTime);
                    //approximate result
                    seconds = Math.ceil(result / 1000);

                    if(utilsIsObjectNullOrUndefined(seconds) || seconds < 0  || utilsIsANumber(seconds)=== false)
                    {
                        seconds = 0;
                    }

                    oDate = new Date(1970, 0, 1);
                    oDate.setSeconds(0 + seconds);
                    //add running time
                    aoProcessesRunning[iIndexNewProcess].timeRunning = oDate;
                }
            }
        }

        return aoProcessesRunning;
    };

    RootController.prototype.findLastProcess = function(aoProcessesRunning)
    {
        if(utilsIsObjectNullOrUndefined(aoProcessesRunning) === true )
        {
            return null;
        }
        var oLastProcessRunning = null;
        var iTotalProcessesNumber = aoProcessesRunning.length;
        // Search the last one that is in running state
        for( var  iIndexNewProcess= 0; iIndexNewProcess < iTotalProcessesNumber; iIndexNewProcess++) {
            if (aoProcessesRunning[iIndexNewProcess].status === "RUNNING"||
                aoProcessesRunning[iIndexNewProcess].status === "WAITING"||
                aoProcessesRunning[iIndexNewProcess].status === "READY") {
                oLastProcessRunning = aoProcessesRunning[iIndexNewProcess];
            }
        }
        return oLastProcessRunning;
    };

    RootController.prototype.getSummary = function()
    {
        var oController = this;
        this.m_oProcessesLaunchedService.getSummary()
            .success(function (data, status) {
                if(utilsIsObjectNullOrUndefined(data) === true || data.BoolValue === false)
                {
                    utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET SUMMARY");
                }
                else
                {
                    oController.m_oSummary = data;
                    oController.m_iNumberOfProcesses = data.userProcessRunning + data.userDownloadRunning + data.userIDLRunning;
                    oController.m_iWaitingProcesses = data.userProcessWaiting + data.userDownloadWaiting + data.userIDLWaiting;
                }

            })
            .error(function (data,status) {
                utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN GET SUMMARY");

            });
    };

    RootController.prototype.getConnectionStatusForTooltip = function()
    {
        //return Math.random() * 1000;
        if( this.isRabbitConnected() == true){ return "Connected"}
        return "Disconnected";
    }

    RootController.prototype.initTooltips = function()
    {
        var _this = this;
        var tooltipsList = $('[data-toggle="tooltip"]');
        if(tooltipsList.length == 0)
        {
            setTimeout(function () {
                _this.initTooltips();
            }, 100);
            return;
        }

        tooltipsList.tooltip();
    }


    RootController.prototype.openLogoutModal = function()
    {
        $('#logoutModal').modal('show');
    }
    RootController.prototype.closeLogoutModal = function()
    {
        $('#logoutModal').modal('hide');
    }

    RootController.prototype.isRabbitConnected = function()
    {
        //console.debug("Rabbit is connected?", this.m_isRabbitConnected);
        return this.m_isRabbitConnected;
        // return false;
        // return true;
    }

    RootController.prototype.onClickProcess = function()
    {
        var oController=this;
        oController.m_bProcessMenuIsVisible = !oController.m_bProcessMenuIsVisible;
    };

    RootController.prototype.onClickLogOut = function()
    {
        var _this = this;

        //this.openLogoutModal();
        this.m_oAuthService.logout()
            .success(function (data, status) {
                if(utilsIsObjectNullOrUndefined(data) === true || data.BoolValue === false)
                {
                    utilsVexDialogAlertTop("SERVER ERROR ON LOGOUT");
                    console.log("SERVER ERROR ON LOGOUT");
                }

                try
                {
                    _this.m_oConstantsService.setActiveWorkspace(null);
                    _this.m_oConstantsService.logOut();
                }catch(e)
                {

                }

                //_this.closeLogoutModal();
                _this.m_oState.go("home");
            })
            .error(function (data,status) {
                utilsVexDialogAlertTop("ERROR IN LOGOUT");
                _this.m_oConstantsService.logOut();
                //_this.closeLogoutModal();
                _this.m_oState.go("home");
            });


    };

    RootController.prototype.getWorkspaceName = function()
    {
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();

        if(utilsIsObjectNullOrUndefined(oWorkspace)) return "";
        else
        {
            if(utilsIsObjectNullOrUndefined(oWorkspace.name)) return "";
            else return oWorkspace.name;
        }

    };

    RootController.prototype.noActiveLinkInNavBarCSS = function()
    {
        return ".not-active { pointer-events: none; cursor: default;}";
    };

    RootController.prototype.openWorkspace = function (sWorkspaceId) {

        var oController = this;

        this.m_oWorkspaceService.getWorkspaceEditorViewModel(sWorkspaceId).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    oController.m_oConstantsService.setActiveWorkspace(data);
                    oController.m_sWorkSpace = oController.m_oConstantsService.getActiveWorkspace();
                }
            }
        }).error(function (data,status) {
            utilsVexDialogAlertTop("GURU MEDITATION<br>ERROR IN OPEN WORKSPACE");
        });
    };

    /***************************** IS VISIBLE HTML ELEMENT ******************************/

    RootController.prototype.isProcessesBarVisible = function ()
    {
        var sState = this.m_oState.current.name;

        // NOTE: a bug is reported using 'ng-class' in combination with 'ui-view'
        // (https://github.com/angular-ui/ui-router/issues/866)
        // Try to solve with jQuery workaround

        if( sState == "root.workspaces" || sState == "root.marketplace"  || sState == "root.appdetails" || sState == "root.appui")
        {
            $("#main").removeClass("has-processes-bar");
            return false;
        }

        $("#main").addClass("has-processes-bar");
        return true;
    };

    RootController.prototype.isVisibleNavBar=function()
    {
        var sState=this.m_oState.current.name;
        if (sState=="root.workspaces") {
            return false;
        }else{
            return true;
        }

    };

    RootController.prototype.disableEditorButton = function(){
        return (utilsIsObjectNullOrUndefined(this.m_oConstantsService.getActiveWorkspace()));
    };

    RootController.prototype.hideWorkspaceName = function(){
        var sState = this.m_oState.current.name;
        if(sState !== "root.editor") return true;
        return false;
    };


    RootController.prototype.cursorCss = function(){
        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(oWorkspace) === true || utilsIsObjectNullOrUndefined(oWorkspace.workspaceId)=== true )
            return "no-drop";
        else
            return "pointer";
    };

    /*********************************************************************************/
    /* ***************** OPEN LINK *****************/
    RootController.prototype.openEditorPage = function () {

        //if(this.isWorkspacesPageOpen() === true) return false;

        var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        if(utilsIsObjectNullOrUndefined(oWorkspace.workspaceId)) return false;
        var oController = this;
        var sWorkSpace = this.m_oConstantsService.getActiveWorkspace();
        oController.m_oState.go("root.editor", { workSpace : sWorkSpace.workspaceId });//use workSpace when reload editor page
    };

    RootController.prototype.openCatalogPage = function()
    {
        this.m_oState.go("root.catalog", { });
    };

    RootController.prototype.openSearchorbit = function()
    {
        this.m_oState.go("root.searchorbit", { });
    };

    RootController.prototype.openMarketPlace = function()
    {
        this.m_oState.go("root.marketplace", { });
    };

    RootController.prototype.openImportPage = function () {
        var oController = this;

        oController.m_oState.go("root.import", { });// workSpace : sWorkSpace.workspaceId use workSpace when reload editor page
    };

    RootController.prototype.activePageCss = function(oPage)
    {
        return (oPage == this.m_oState.current.name );
    };

    RootController.prototype.toggleProcessesBar = function()
    {
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = !this.m_bIsOpenNav;
    }

    RootController.prototype.openNav = function() {
        //document.getElementById("status-bar").style.height = "500px";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = true;

    };

    RootController.prototype.closeNav = function() {
        //document.getElementById("status-bar").style.height = "4.5%";
        this.m_bIsOpenStatusBar = !this.m_bIsOpenStatusBar;
        this.m_bIsOpenNav = false;

    };


    RootController.prototype.openSnake = function()
    {
        var oController = this;
        // console.log("miao");
        this.m_oModalService.showModal({
            // templateUrl: "dialogs/snake_dialog/SnakeDialog.html",
            templateUrl: "dialogs/snake_dialog/SnakeDialogV2.html",
            controller: "SnakeController"
        }).then(function(modal) {
            modal.element.modal();

            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    };

    RootController.prototype.deleteProcess = function(oProcessInput)
    {

        // var oController = this;
        // var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        // this.m_oModalService.showModal({
        //     templateUrl: "dialogs/delete_process/DeleteProcessDialog.html",
        //     controller: "DeleteProcessController"
        // }).then(function(modal) {
        //     modal.element.modal();
        //     modal.close.then(function(result) {
        //         oController.m_oScope.Result = result ;
        //
        //         if(result === 'delete')
        //             oController.m_oProcessesLaunchedService.removeProcessInServer(oProcessInput.processObjId,oWorkspace.workspaceId,oProcessInput)
        //     });
        // });
        this.m_oProcessesLaunchedService.deleteProcess(oProcessInput);
        return true;
    };

    /**
     * openProcessLogsDialog
     * @returns {boolean}
     */
    RootController.prototype.openProcessLogsDialog = function()
    {
        var oController = this;
        // var oWorkspace = this.m_oConstantsService.getActiveWorkspace();
        this.m_oModalService.showModal({
            templateUrl: "dialogs/processes_logs/ProcessesLogsDialog.html",
            controller: "ProcessesLogsController"
        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
            });
        });

        return true;
    };

    RootController.prototype.editModelWorkspaceNameSetTrue = function(){

        var oController = this;
        if  (utilsIsObjectNullOrUndefined(oController.m_oConstantsService.getActiveWorkspace())) return;

        var oCallback = function (value) {

            if (utilsIsObjectNullOrUndefined(value))
            {
                return;
            }
            if( value === "Untitled Workspace" )
            {
                value = oController.forcedChangeNameWorkspace();
            }
            var oWorkspace = oController.m_oConstantsService.getActiveWorkspace();
            oWorkspace.name = value;

            oController.m_oWorkspaceService.UpdateWorkspace(oWorkspace).success(function (data) {
                oWorkspace.name = data.name
                oController.m_bIsEditModelWorkspaceNameActive = false;
            });
        };
        utilsVexPrompt("Insert Workspace Name:<br>", oController.m_oConstantsService.getActiveWorkspace().name, oCallback);

        this.m_bIsEditModelWorkspaceNameActive = true;
    };

    RootController.prototype.editUserInfo = function(oProcessInput)
    {
        var oController = this;

        this.m_oModalService.showModal({
            templateUrl: "dialogs/edit_user/EditUserDialog.html",
            controller: "EditUserController",
            inputs: {
                extras: {
                    user:oController.m_oUser
                }
            }
        }).then(function(modal) {
            modal.element.modal();
            modal.close.then(function(result) {
                oController.m_oScope.Result = result ;
                oController.m_oUser = oController.m_oConstantsService.getUser();
                // if(result === 'delete')
                //     oController.m_oProcessesLaunchedService.removeProcessInServer(oProcessInput.processObjId,oWorkspace.workspaceId,oProcessInput)
            });
        });

        return true;
    };

    RootController.prototype.getWorkspacesInfo = function()
    {
        if (this.m_oConstantsService.getUser() != null) {
            if (this.m_oConstantsService.getUser() != undefined) {

                var oController = this;

                this.m_oWorkspaceService.getWorkspacesInfoListByUser().success(function (data, status) {
                    if (utilsIsObjectNullOrUndefined(data) === false)
                    {
                        oController.m_aoWorkspaces = data;
                    }

                }).error(function (data,status) {
                    utilsVexDialogAlertTop('GURU MEDITATION<br>ERROR IN WORKSPACESINFO');

                });
            }
        }
    }

    RootController.prototype.forcedChangeNameWorkspace = function () {
        //Untitled Workspace
        var sName="Untitled Workspace";

        if(utilsIsObjectNullOrUndefined(this.m_aoWorkspaces) === true)
        {
            return sName;
        }
        var iNumberOfWorkspaces = this.m_aoWorkspaces.length;
        var iIndexReturnValue = 1;
        var sReturnValue = sName + "(" + iIndexReturnValue + ")";
        for(var iIndexWorkspace = 0 ; iIndexWorkspace < iNumberOfWorkspaces ; iIndexWorkspace++ )
        {

            if(this.m_aoWorkspaces[iIndexWorkspace].workspaceName === sReturnValue)
            {
                iIndexReturnValue++;
                sReturnValue = sName + "(" + iIndexReturnValue + ")";
            }
        }

        return sReturnValue;
    };

    RootController.prototype.openErrorLogsDialog = function(oProcess)
    {

        var oController = this;

        if(utilsIsObjectNullOrUndefined(oProcess) === true)
        {
            return false;
        }
        oController.m_oModalService.showModal({
            templateUrl: "dialogs/process_error_logs_dialog/ProcessErrorLogsDialogView.html",
            controller: "ProcessErrorLogsDialogController",
            inputs: {
                extras: {
                    process:oProcess,
                }
            }
        }).then(function (modal) {
            modal.element.modal();
            modal.close.then(function(oResult){

            });
        });
        return true;
    };

    RootController.prototype.openLogsDialogByProcessId = function(processId)
    {

        var oController = this;

        if(!processId)
        {
            return false;
        }

        let process = null;
        for(let i = 0; i < oController.m_aoProcessesRunning.length; i++)
        {
            if(processId == oController.m_aoProcessesRunning[i].processObjId){
                process = oController.m_aoProcessesRunning[i];
                break;
            }
        }

        if( process != null){
            oController.openErrorLogsDialog(process);
            return true;
        }else{
            console.error("Cannot find process ID " + processId + " in the processes list")
            return false;
        }
    };

    RootController.prototype.openDocumentatonCenter = function () {
        this.m_oWindow.open('http://www.wasdi.net/index.php', '_blank');
    }

    /*********************************************************************/
    RootController.$inject = [
        '$scope',
        'ConstantsService',
        'AuthService',
        '$state',
        'ProcessesLaunchedService',
        'WorkspaceService',
        '$timeout',
        'ModalService',
        'RabbitStompService',
        '$window'
    ];

    return RootController;
}) ();
