package wasdi.snapopearations;

import org.apache.commons.net.io.Util;
import org.esa.s1tbx.io.sentinel1.Sentinel1ProductReader;
import org.esa.snap.core.util.SystemUtils;
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AttributeViewModel;
import wasdi.shared.viewmodels.BandViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;
import wasdi.shared.viewmodels.NodeGroupViewModel;
import wasdi.shared.viewmodels.ProductViewModel;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by s.adamo on 18/05/2016.
 */
public class ReadProduct {

    public  static HashMap<String, Product> m_oCacheProducts = new HashMap<String, Product>();


    public Product ReadProduct(File oFile, String sFormatName) {
        //Per ora ipotizziamo solo dati Sentinel-1
        //read product
        Product exportProduct = null;

        try {

            if (m_oCacheProducts.get(oFile.getName()) == null) {
                LauncherMain.s_oLogger.debug("ReadProduct.ReadProduct: begin read");

                if (sFormatName != null) {
                    exportProduct = ProductIO.readProduct(oFile, sFormatName);
                } else {
                    exportProduct = ProductIO.readProduct(oFile);
                }
            }
            //put in cache dictionary
            m_oCacheProducts.put(oFile.getName(), exportProduct);
        } catch (Exception oEx) {
            oEx.printStackTrace();
            LauncherMain.s_oLogger.debug(oEx.toString());

        }


        return m_oCacheProducts.get(oFile.getName());
    }

    private boolean RemoveFromCache(File oFile) {
        if (m_oCacheProducts.get(oFile.getName()) != null) {
            m_oCacheProducts.remove(oFile.getName());
            return  true;
        }

        return  false;
    }

    public ProductViewModel getProductViewModel(File oFile) throws IOException
    {
        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: start");

        ProductViewModel oViewModel = new ProductViewModel();
        Product exportProduct = ReadProduct(oFile, null);

        if (exportProduct == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: read product returns null");
            return null;
        }

        // P.Campanella: splitted bands and metadata view models
        //oViewModel.setMetadata(GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata")));

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: call fill bands view model");

        this.FillBandsViewModel(oViewModel, exportProduct);

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: setting Name and Path");

        oViewModel.setName(Utils.GetFileNameWithoutExtension(oFile.getAbsolutePath()));
        oViewModel.setFileName(oFile.getName());

        LauncherMain.s_oLogger.debug("ReadProduct.getProductViewModel: end");

        return  oViewModel;
    }

    public MetadataViewModel getProductMetadataViewModel(File oFile) throws IOException
    {
        Product exportProduct = ReadProduct(oFile, null);

        if (exportProduct == null) return null;

        return  GetMetadataViewModel(exportProduct.getMetadataRoot(), new MetadataViewModel("Metadata"));
    }

    private MetadataViewModel GetMetadataViewModel(MetadataElement oElement, MetadataViewModel oSourceViewModel) {

        for (MetadataAttribute oMetadataAttribute : oElement.getAttributes()) {
            AttributeViewModel oAttributeViewModel = new AttributeViewModel();
            oAttributeViewModel.setName(oMetadataAttribute.getName());
            oAttributeViewModel.setDescription(oMetadataAttribute.getDescription());
            if (oSourceViewModel.getAttributes() == null)
                oSourceViewModel.setAttributes(new ArrayList<AttributeViewModel>());
            oSourceViewModel.getAttributes().add(oAttributeViewModel);
        }


        for (MetadataElement oMetadataElement : oElement.getElements()) {
            MetadataViewModel oElementViewModel = new MetadataViewModel(oMetadataElement.getName());
            if (oSourceViewModel.getElements() == null)
                oSourceViewModel.setElements(new ArrayList<MetadataViewModel>());
            oSourceViewModel.getElements().add(GetMetadataViewModel(oMetadataElement, oElementViewModel));
        }

        return  oSourceViewModel;
    }

    private void FillBandsViewModel(ProductViewModel oProductViewModel, Product oProduct)
    {
        if (oProductViewModel == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: ViewModel null return");
            return;
        }

        if (oProduct == null) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: Product null");
            return;
        }

        if (oProductViewModel.getBandsGroups() == null) oProductViewModel.setBandsGroups(new NodeGroupViewModel("Bands"));

        for (Band oBand : oProduct.getBands()) {
            LauncherMain.s_oLogger.debug("ReadProduct.FillBandsViewModel: add band " + oBand.getName());

            if (oProductViewModel.getBandsGroups().getBands() == null)
                oProductViewModel.getBandsGroups().setBands(new ArrayList<BandViewModel>());

            BandViewModel oViewModel = new BandViewModel(oBand.getName());
            oProductViewModel.getBandsGroups().getBands().add(oViewModel);
        }

    }

    public String writeBigTiff(String sFileName, String sWorkingPath, String sFileNameWithoutExtension) throws Exception {

        File oFile = new File (sFileName);

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Read Product FILE = " + sFileName);

        Product oSentinelProduct = ReadProduct(oFile, null);

        if (oSentinelProduct == null) LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Sentinel Product is null " + oFile.getAbsolutePath());

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: Create Writer");

        WriteProduct oWriter = new WriteProduct();

        LauncherMain.s_oLogger.debug("ReadProduct.writeBigTiff: WriteTiff");

        String sBigTiff = "";

        try {
            sBigTiff = oWriter.WriteBigTiff(oSentinelProduct, sWorkingPath, sFileNameWithoutExtension);
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        // Close product IO
        RemoveFromCache(oFile);
        oSentinelProduct.closeIO();

        return sBigTiff;
    }

    public Product ReadProduct(File oFile) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(oFile);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        }finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        Object oObjectProduct = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(ous.toByteArray());
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            oObjectProduct = in.readObject();

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }

        return (Product) oObjectProduct;
    }



}
