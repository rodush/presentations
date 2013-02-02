//package com.rodush.examples.scraper;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.attachment.CollectingAttachmentHandler;
import com.gargoylesoftware.htmlunit.attachment.Attachment;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MyScraper
{
    public static void main( String[] args ) throws java.io.IOException
    {

        //System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
        // Log only fatal errors
        System.getProperties().put( "org.apache.commons.logging.simplelog.defaultlog", "fatal" );


        //LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        //java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
        //java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);



        final WebClient client = new WebClient( BrowserVersion.FIREFOX_10 );
        client.getOptions().setThrowExceptionOnFailingStatusCode( false );
        client.getOptions().setUseInsecureSSL( true );
        
        // Attach attachment handler
        client.setAttachmentHandler( new CollectingAttachmentHandler() );

        // Start our scraping
        HtmlPage page = client.getPage( "http://rutor.org/" );

        // Get to the Search page
        HtmlAnchor linkToSearchPage = page.getFirstByXPath( "//a[@href='/search/' and  @class='menu_b']" );
        if( linkToSearchPage != null ){
            page = linkToSearchPage.click();
            // TODO: Save page as XML document
            String pageHtml = page.asXml();
        }
        else {
            System.out.println( "Can't find link to Search page" ); 
            return;
        }

        List<WebWindow> windowList = (List<WebWindow>) client.getWebWindows();
        Iterator windowIt = windowList.iterator();
        while( windowIt.hasNext() ){
            WebWindow aWindow = (WebWindow) windowIt.next();
            HtmlPage aPage = (HtmlPage) aWindow.getEnclosedPage();
            System.out.println( "Current window has page with title: " + aPage.getTitleText().trim() );
            if( aPage.getTitleText().contains( "Поиск" ) ){
                System.out.println( "Okay, we have found our page, keep reference to it" );
                page = aPage;
            }
        }
        
        int openedWindowsCnt = client.getWebWindows().size();
        System.out.println( "We have got " + openedWindowsCnt + " windows opened" );
        System.out.println( "Looking in the page with title: " + page.getTitleText().trim() );
        // Find form with search input tag
        HtmlForm searchForm = (HtmlForm) page.getFirstByXPath( "//*[@id='content']/fieldset/form" );

        if( searchForm == null ){
            System.out.println( "Can't find search form..." ); 
            return;
        }

        HtmlInput searchInput = (HtmlInput) page.getElementById( "inputtext_id" );
        // TODO: Change categoty to "Сериалы"
         
        searchInput.type( "Californication" );
        String currentPageXml = page.asXml();
        // TODO: Save page - ensure that the value was typed
        
        HtmlSubmitInput submitBtn = (HtmlSubmitInput) searchForm.getInputByValue( "Поехали" );
        System.out.println( "Submitting search form");
        submitBtn.click();
        client.waitForBackgroundJavaScriptStartingBefore( 2000 );
        page = (HtmlPage) client.getCurrentWindow().getEnclosedPage();
        currentPageXml = page.asXml();

        // Get the very first link to torrent
        HtmlAnchor linkToTorrentPage = (HtmlAnchor) page.getFirstByXPath("//a[contains(@href, '/torrent')]");
        System.out.println( "Clicking found link -  " + linkToTorrentPage.asText() );
        page = linkToTorrentPage.click();
        currentPageXml = page.asXml();
        // TODO: Save page source
        
        // Find link to torrent file
        System.out.println( "Got page with title '" + page.getTitleText().trim() + "'" );
        HtmlAnchor linkToTorrentFile = (HtmlAnchor) page.getFirstByXPath( "//div[@id='download']//a[contains(text(), 'Скачать ')]" );
        if( linkToTorrentFile == null ){
            System.out.println( "Can't find Download link :( ...." );
            return;
        }
        linkToTorrentFile.click();

        // or.... 
        // you could just use client.getWebResponse().getContentAsString() 
        // and put it into file with an extension you expect to get

        // Here starts downloading of this file...
        List<Attachment> attachments = ((CollectingAttachmentHandler) client.getAttachmentHandler()).getCollectedAttachments();
        System.out.println( "We have got " + attachments.size() + " attachments" );
        System.out.println( "Attached filename - " + attachments.get(0).getSuggestedFilename() );
        System.out.println( "Attached page content length - " + attachments.get(0).getPage().getWebResponse().getContentAsString().length() + " chars" );

        // TODO: Save file on the disc
        File torrentFile = new File( attachments.get(0).getSuggestedFilename() );
        if( !torrentFile.exists() ){
            torrentFile.createNewFile();
        }

        try{
            FileWriter fw = new FileWriter(torrentFile.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write( attachments.get(0).getPage().getWebResponse().getContentAsString() );
            bw.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            System.out.println( "Failed save torrent file to disc. Reason: " + ioe.getMessage() );
        }

        System.out.println( "Done!" );

        //System.out.println( "Here is content of our page: \n\n\n" + currentPageXml + "\n\n\n" );
        return;
    }

}
