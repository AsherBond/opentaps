/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentaps.common.event;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.foundation.action.ActionContext;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * The actions used to support the features on login screen.
 */
public class LoginScreenActions {

    private static final String MODULE = LoginScreenActions.class.getName();
    private static final String RSS_URL = "http://www.opentaps.org/news/rss";
    private static final String NEWS_URL = "http://www.opentaps.org/news";
    private static int MAX_DISPLAY_CHARS = 100;
    private static int MAX_DISPLAY_RECORDS = 5;

    /**
     * Action for the display latest news feature.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void getLatestNews(Map<String, Object> context) throws GeneralException {
        final ActionContext ac = new ActionContext(context);

        try {
            URL url = new URL(RSS_URL);
            XmlReader reader = new XmlReader(url);
            SyndFeedInput input = new SyndFeedInput();  
            SyndFeed feed = input.build(reader);  
            List entries = feed.getEntries();
            if (entries.size() > 0) {
                List latestnews = new FastList();
                for (int i=0; i < entries.size() && i < MAX_DISPLAY_RECORDS; i++) {
                    HashMap<String, String> news = new HashMap<String, String>();
                    SyndEntry entry = (SyndEntry) entries.get(i);
                    news.put("title", getIntro(entry.getTitle()));
                    news.put("link", getIntro(entry.getLink()));
                    SyndContent description = entry.getDescription();
                    if (description != null) {
                        news.put("description", getIntro(description.getValue()));
                    }
                    DateFormat dateFormat =  new SimpleDateFormat(UtilDateTime.getDateTimeFormat(ac.getLocale()));
                    String dateString = dateFormat.format(entry.getPublishedDate());
                    news.put("publishedDate", dateString);
                    latestnews.add(news);
                }
                ac.put("latestnews", latestnews);
                ac.put("newsUrl", NEWS_URL);
            }

        } catch (UnknownHostException e) {
            // we cannot connect the internet, so just ignore the exception
            Debug.logError(e, MODULE);
        } catch (MalformedURLException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(e);
        } catch (FeedException e) {
            Debug.logError(e.getMessage(), MODULE);
        }
    }

    /**
     * Returns first 100 characters of the news body 
     */
    public static String getIntro(String description) {
        if (UtilValidate.isEmpty(description)) {
            return description;
        }
        if (description.length() <= MAX_DISPLAY_CHARS) {
            return description;
        } else {
            return description.substring(0, MAX_DISPLAY_CHARS) + "...";
        }
    }
}
