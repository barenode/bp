package pdf;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import java.io.File;
import java.io.IOException;

public class PDFMerge {

    public static void main(String[] args) throws Exception{

        try {

            PDFMergerUtility pdfMerger = new PDFMergerUtility();
            pdfMerger.setDestinationFileName("./merged.pdf");

            //PDDocumentInformation documentInformation = new PDDocumentInformation();
            //documentInformation.setTitle("Apache PdfBox Merge PDF Documents");
            //documentInformation.setCreator("memorynotfound.com");
            //documentInformation.setSubject("Merging PDF documents with Apache PDF Box");

            pdfMerger.addSource(new File("./pdf/header.pdf"));
            pdfMerger.addSource(new File("./book.pdf"));

            pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        } catch (IOException e){
            System.err.println("Exception while trying to merge pdf document - " + e);
        }
    }

}