//package com.rodush.examples.scraper;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;


public class MyScraper
{
    public static void main( String[] args ) throws java.io.IOException
    {

        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");

        //LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        //java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 
        //java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);



        final WebClient client = new WebClient( BrowserVersion.FIREFOX_10 );
        client.getOptions().setThrowExceptionOnFailingStatusCode( false );
        client.getOptions().setUseInsecureSSL( true );
        HtmlPage page = client.getPage( "http://rodush.com/" );
        
        System.out.println( "Here is the title comes - " + page.getTitleText().trim() + "\n\n" );
        final HtmlAnchor link = page.getFirstByXPath( "//h2[@class='entry-title']/a[@rel='bookmark']" );
        if ( link == null )
        {
            System.out.println( "Link was not found...." );
            return;
        }
        System.out.println( "Get the very first link to the publication: " + link.asText() );
        page = link.click();
        System.out.println( "Now page's title is: " + page.getTitleText() );
    }

}
