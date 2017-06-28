/**
 * Created by a.corrado on 24/05/2017.
 */



var SftpUploadController = (function() {

    function SftpUploadController($scope, oClose,oExtras,oAuthService,oConstantsService) {
        this.m_oScope = $scope;
        this.m_oScope.m_oController = this;
        this.m_oOrbit = oExtras;
        this.m_oAuthService = oAuthService;
        this.m_aoListOfFiles = [];
        this.m_bIsAccountCreated = null;
        this.m_sEmailNewPassword="";
        this.m_sEmailNewUser="";
        this.m_oConstantsService = oConstantsService;
        this.m_oUser = this.m_oConstantsService.getUser();
        this.m_bIsVisibleLoadIcon = false;
        this.m_aoSelectedFiles = [];
        //$scope.close = oClose;
        $scope.close = function(result) {
            oClose(result, 500); // close, but give 500ms for bootstrap to animate
        };

        this.isCreatedAccountUpload();
        // this.getListFiles();
        // this.createAccountUpload();
    }

    SftpUploadController.prototype.deleteSftpAccount = function(){
        //TODO chec if there is the user
        if(utilsIsObjectNullOrUndefined(this.m_oUser)=== true ||utilsIsObjectNullOrUndefined(this.m_oUser.userId)=== true)
            return false;
        var oController = this;
        this.m_bIsVisibleLoadIcon = true;
        this.m_oAuthService.deleteAccountUpload(this.m_oUser.userId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oController.isCreatedAccountUpload();
                    oController.m_bIsVisibleLoadIcon = false;
                }
            }
        }).error(function (data, status) {
            if(data) console.log("SftpUploadController error during delete account");
            this.m_bIsVisibleLoadIcon = false;
        });
        return true;
    };
    SftpUploadController.prototype.updateSftpPassword = function(){
        if(utilsIsObjectNullOrUndefined(this.m_sEmailNewPassword) === true || utilsIsStrNullOrEmpty(this.m_sEmailNewPassword) === true )
            return false;
        this.m_bIsVisibleLoadIcon = true;
        var oController = this;
        this.m_oAuthService.updatePasswordUpload(this.m_sEmailNewPassword).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {

                }
            }
            oController.m_bIsVisibleLoadIcon = false;
        }).error(function (data, status) {
            if(data)
                console.log("SftpUploadController error during creation new password");
            oController.m_bIsVisibleLoadIcon = false;
        });

        return true;
    };

    SftpUploadController.prototype.createAccountUpload = function()
    {
        if(utilsIsObjectNullOrUndefined(this.m_sEmailNewUser) === true || utilsIsStrNullOrEmpty(this.m_sEmailNewUser) === true || utilsIsEmail(this.m_sEmailNewUser)=== false )
            return false;
        var oController = this;
        // var sTestEmail = "a.corrado@fadeout.it";
        this.m_bIsVisibleLoadIcon = true;
        this.m_oAuthService.createAccountUpload(this.m_sEmailNewUser).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oController.isCreatedAccountUpload();

                }
            }
            oController.m_bIsVisibleLoadIcon = false;
        }).error(function (data, status) {
            if(data) console.log("SftpUploadController error during creation");
            oController.m_bIsVisibleLoadIcon = false;
        });
    };

    SftpUploadController.prototype.isCreatedAccountUpload = function()
    {
        var oController = this;
        this.m_oAuthService.isCreatedAccountUpload().success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    if(data === false) {
                        oController.m_bIsAccountCreated = false;
                    }
                    else
                    {
                        oController.m_bIsAccountCreated = true;
                        oController.getListFiles();
                    }
                }
            }
        }).error(function (data, status) {
            if(data)
                console.log("SftpUploadController error during check if the account is created");
        });
    }
    SftpUploadController.prototype.getListFiles = function ()
    {
        var oThat = this;
        this.m_oAuthService.getListFilesUpload().success(function (data, status) {
            if (data != null) {
                if (data != undefined) {
                    oThat.m_aoListOfFiles = data;
                }
            }
        }).error(function (data, status) {
            if(data)
                console.log("SftpUploadController error during get-list");
        });
    };

    SftpUploadController.prototype.isVisibleLoadIcon = function(){
        if(utilsIsObjectNullOrUndefined(this.m_bIsAccountCreated) === true || this.m_bIsVisibleLoadIcon === true)
        {
            return true;
        }
        else{
            return false;
        }
    };

    SftpUploadController.prototype.isAccountCreated = function(){
        if( this.m_bIsAccountCreated)
        {
            return true;
        }
        else{
            return false;
        }

    };

    SftpUploadController.prototype.ingestAllSelectedFiles = function () {
        var iSelectedFilesLength = this.m_aoSelectedFiles.length;

        for(var iIndexSelectedFile = 0; iIndexSelectedFile < iSelectedFilesLength ; iIndexSelectedFile++){
            if(this.ingestFile(this.m_aoSelectedFiles[iIndexSelectedFile]) === false)
                console.log("Something doesn't work during ingest File, indexFile:" + iIndexSelectedFile);
        }
    };

    /**
     * ingestFile
     * @param oSelectedFile
     * @returns {boolean}
     */
    SftpUploadController.prototype.ingestFile = function(oSelectedFile){
        if(utilsIsObjectNullOrUndefined(oSelectedFile)=== true )
            return false;
        this.m_oAuthService.ingestFile(oSelectedFile,this.m_oConstantsService.getActiveWorkspace().workspaceId).success(function (data, status) {
            if (data != null) {
                if (data != undefined) {

                }
            }
        }).error(function (data, status) {
            if(data)
            {
                console.log("SftpUploadController error during ingest file");
                utilsVexDialogAlertTop("During the ingestion file there was an error, file:" + oSelectedFile);
            }
        });
        return true;
    };

    SftpUploadController.$inject = [
        '$scope',
        'close',
        'extras',
        'AuthService',
        'ConstantsService'
    ];
    return SftpUploadController;
})();
