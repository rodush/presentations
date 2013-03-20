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

import java.util.Scanner;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyScraper
{
    static private FileHandler fileTxt;
    static private SimpleFormatter formatterTxt;
    static private Logger myLogger = Logger.getLogger(MyScraper.class.getName());

    public static void main( String[] args ) throws java.io.IOException
    {
        String command;
        Scanner scanIn = new Scanner(System.in);

        //Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        Logger.getLogger("com.gargoylesoftware.htmlunit.WebClient").setLevel(Level.INFO);
        Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);

        myLogger.setLevel(Level.INFO);
        fileTxt = new FileHandler("logging.txt");

        // Create txt formatter
        formatterTxt = new SimpleFormatter();
        fileTxt.setFormatter(formatterTxt);
        myLogger.addHandler(fileTxt);

        //System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
        // Log only fatal errors
        //System.getProperties().put( "org.apache.commons.logging.simplelog.defaultlog", "fatal" );

        //LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");


        //-----------------------------------------------------------------
        // INSTANTIATE A WEB CLIENT
        //-----------------------------------------------------------------

        final WebClient client = new WebClient( BrowserVersion.FIREFOX_10 );

        client.getOptions().setThrowExceptionOnFailingStatusCode( false );
        client.getOptions().setUseInsecureSSL( true );
        client.getOptions().setJavaScriptEnabled( true );

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
            myLogger.info( "Can't find link to Search page" ); 
            return;
        }

        List<WebWindow> windowList = (List<WebWindow>) client.getWebWindows();
        Iterator windowIt = windowList.iterator();
        while( windowIt.hasNext() ){
            WebWindow aWindow = (WebWindow) windowIt.next();
            HtmlPage aPage = (HtmlPage) aWindow.getEnclosedPage();
            myLogger.info( "Current window has page with title: " + aPage.getTitleText().trim() );
            if( aPage.getTitleText().contains( "Поиск" ) ){
                myLogger.info( "Okay, we have found our page, keep reference to it" );
                page = aPage;
            }
        }

        int openedTopWindowsCnt = client.getTopLevelWindows().size();
        myLogger.info( "We have got " + openedTopWindowsCnt + " Top-Level windows opened" );

        
        int openedWindowsCnt = client.getWebWindows().size();
        myLogger.info( "We have got " + openedWindowsCnt + " Web-Windows opened" );
        myLogger.info( "Looking in the page with title: " + page.getTitleText().trim() );
        // Find form with search input tag
        HtmlForm searchForm = (HtmlForm) page.getFirstByXPath( "//*[@id='content']/fieldset/form" );

        if( searchForm == null ){
            myLogger.info( "Can't find search form..." ); 
            return;
        }

        HtmlInput searchInput = (HtmlInput) page.getElementById( "inputtext_id" );
        // TODO: Change categoty to "Сериалы"
         
        searchInput.type( "Californication" );
        String currentPageXml = page.asXml();
        // TODO: Save page - ensure that the value was typed
        
        HtmlSubmitInput submitBtn = (HtmlSubmitInput) searchForm.getInputByValue( "Поехали" );
        myLogger.info( "Submitting search form");
        submitBtn.click();
        client.waitForBackgroundJavaScriptStartingBefore( 2000 );
        page = (HtmlPage) client.getCurrentWindow().getEnclosedPage();
        currentPageXml = page.asXml();

        // Get the very first link to torrent
        HtmlAnchor linkToTorrentPage = (HtmlAnchor) page.getFirstByXPath("//a[contains(@href, '/torrent')]");
        myLogger.info( "Clicking found link -  " + linkToTorrentPage.asText() );
        page = linkToTorrentPage.click();
        currentPageXml = page.asXml();
        // TODO: Save page source
        
        // Find link to torrent file
        myLogger.info( "Got page with title '" + page.getTitleText().trim() + "'" );
        HtmlAnchor linkToTorrentFile = (HtmlAnchor) page.getFirstByXPath( "//div[@id='download']//a[contains(text(), 'Скачать ')]" );
        if( linkToTorrentFile == null ){
            myLogger.info( "Can't find Download link :( ...." );
            return;
        }
        linkToTorrentFile.click();

        // or.... 
        // you could just use client.getWebResponse().getContentAsString() 
        // and put it into file with an extension you expect to get

        // Here starts downloading of this file...
        List<Attachment> attachments = ((CollectingAttachmentHandler) client.getAttachmentHandler()).getCollectedAttachments();
        myLogger.info( "We have got " + attachments.size() + " attachments" );
        myLogger.info( "Attached filename - " + attachments.get(0).getSuggestedFilename() );
        myLogger.info( "Attached page content length - " + attachments.get(0).getPage().getWebResponse().getContentAsString().length() + " chars" );

        // Save file on the disc
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
            myLogger.info( "Failed save torrent file to disc. Reason: " + ioe.getMessage() );
        }

        myLogger.info( "Done!" );

               
        System.out.println("Continue?");
        command = scanIn.nextLine();
        if(command == "yes"){
            scanIn.close();
        }

        //myLogger.info( "Here is content of our page: \n\n\n" + currentPageXml + "\n\n\n" );

        HtmlPage page2 = (HtmlPage) client.getPage("http://localhost:8000/examples/onchange_handler.html");
        HtmlForm aForm = (HtmlForm) page2.getFormByName("testForm");
        HtmlInput loginInput = (HtmlInput) aForm.getInputByName("login");
        loginInput.setValueAttribute("someText");
        myLogger.info( "Here how page looks like after {setValue}: " + page2.asXml() );
        loginInput.blur();
        loginInput.type("Now we type and check...");
        loginInput.blur();
        myLogger.info( "Here how page looks like after {type}: " + page2.asXml() );

        System.out.println("Continue?");
        command = scanIn.nextLine();
        if(command == "yes"){
            scanIn.close();
        }

        // Here page with two iframes. Let's check WebWindows and TopLevelWindows size
        page = (HtmlPage) client.getPage( "http://localhost:8000/examples/page_with_iframes.html" );
        openedTopWindowsCnt = client.getTopLevelWindows().size();
        int webWindowsCnt = client.getWebWindows().size();
        int framesCnt = ((HtmlPage)client.getCurrentWindow().getEnclosedPage()).getFrames().size();
        myLogger.info( "We have " + openedTopWindowsCnt + " Top Level windows, and " +
            webWindowsCnt + " web windows currently opened, plus " + framesCnt + " frames on the parent page!" );

        System.out.println("Continue?");
        command = scanIn.nextLine();
        if(command == "yes"){
            scanIn.close();
        }

        // Example of alert, confirm and prompt:
        HtmlPage pageWithDialogs = client.getPage( "http://localhost:8000/examples/page_with_iframes.html" );
        myLogger.info( "Look, there is nothing to do if one don't want to ;-)" );

        myLogger.warning("Bye!");
    }

}
