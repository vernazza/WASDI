/**
 * Created by p.campanella on 21/10/2016.
 */

var HomeController = (function() {
    function HomeController($scope, $location, oConstantsService, oAuthService,oState) {
        this.m_oScope = $scope;
        this.m_oLocation  = $location;
        this.m_oConstantsService = oConstantsService;
        this.m_oAuthService = oAuthService;
        this.m_oState=oState;
        this.m_oScope.m_oController=this;
        this.m_bLoginIsVisible = false;//Login in visible after click on logo
        this.m_sUserName = "";
        this.m_sUserPassword = "";

        if(this.m_oConstantsService.isUserLogged())
            this.m_oState.go("root.workspaces");// go workspaces

    }

    HomeController.prototype.moveTo = function (sPath) {
        this.m_oLocation.path(sPath);
    }

    HomeController.prototype.login = function () {
        var oLoginInfo = {};
        var oController = this;
        oLoginInfo.userId = oController.m_sUserName;
        oLoginInfo.userPassword = oController.m_sUserPassword;

        var oConstantsService = oController.m_oConstantsService;

        this.m_oAuthService.login(oLoginInfo).success(function (data, status) {
            if (data != null)
            {
                if (data != undefined)
                {
                    if (data.userId != null)
                    {
                        if (data.userId != "")
                        {
                            oConstantsService.setUser(data);//set user
                            oController.m_oState.go("root.workspaces");// go workspaces
                        }
                    }
                }
            }
        }).error(function (data,status) {
            alert('error');
        });
    }

    HomeController.prototype.getUserName = function () {
        var oUser = this.m_oConstantsService.getUser();

        if (oUser != null)
        {
            if (oUser != undefined)
            {
                var sName = oUser.name;
                if (sName == null) sName = "";
                if (sName == undefined) sName = "";

                if (oUser.surname != null)
                {
                    if (oUser.surname != undefined)
                    {
                        sName += " " + oUser.surname;

                        return sName;
                    }
                }
            }
        }

        return "";
    }

    HomeController.prototype.isUserLogged = function () {
        return this.m_oConstantsService.isUserLogged();
    }

    HomeController.$inject = [
        '$scope',
        '$location',
        'ConstantsService',
        'AuthService',
        '$state'
    ];

    return HomeController;
}) ();
