package com.maple.rssreceiver;

import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.xml.parsers.*;

import me.speind.SpeindAPI;

import org.w3c.dom.*;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;

public class RssItem {
	public String providerVocalizing;
	private String title;
	private String description;
	private Date pubDate;
	private String link;
	private String sender;
	private String senderBmpURL;
	private String bmpURL;
	private String lang;

	private static HashMap<String,String> htmlEntities;
	  static {
	    htmlEntities = new HashMap<String,String>();
	    htmlEntities.put("&lsquo;","‘");
	    htmlEntities.put("&rsquo;","’");
	    htmlEntities.put("&sbquo;","‚");
	    htmlEntities.put("&ldquo;","“");
	    htmlEntities.put("&rdquo;","”");
	    htmlEntities.put("&bdquo;","„");
	    htmlEntities.put("&dagger;","†");
	    htmlEntities.put("&Dagger;","‡");
	    htmlEntities.put("&permil;","‰");
	    htmlEntities.put("&lsaquo;","‹");
	    htmlEntities.put("&rsaquo;","›");
	    htmlEntities.put("&spades;","♠");
	    htmlEntities.put("&clubs;","♣");
	    htmlEntities.put("&hearts;","♥");
	    htmlEntities.put("&diams;","♦");
	    htmlEntities.put("&oline;","‾");
	    htmlEntities.put("&larr;","←");
	    htmlEntities.put("&uarr;","↑");
	    htmlEntities.put("&rarr;","→");
	    htmlEntities.put("&darr;","↓");
	    htmlEntities.put("&trade;","™");
	    htmlEntities.put("&#x2122;","™");
	    htmlEntities.put("&#33;","!");
	    htmlEntities.put("&quot;","\"");
	    htmlEntities.put("&#34;","\"");
	    htmlEntities.put("&#35;","#");
	    htmlEntities.put("&#36;","$");
	    htmlEntities.put("&#37;","%");
	    htmlEntities.put("&amp;","&");
	    htmlEntities.put("&#38;","&");
	    htmlEntities.put("&#39;","'");
	    htmlEntities.put("&#40;","(");
	    htmlEntities.put("&#41;",")");
	    htmlEntities.put("&#42;","*");
	    htmlEntities.put("&#43;","+");
	    htmlEntities.put("&#44;",",");
	    htmlEntities.put("&#45;","-");
	    htmlEntities.put("&#46;",".");
	    htmlEntities.put("&frasl;","/");
	    htmlEntities.put("&#47;","/");
	    htmlEntities.put("&#58;",":");
	    htmlEntities.put("&#59;",";");
	    htmlEntities.put("&lt;","<");
	    htmlEntities.put("&#60;","<");
	    htmlEntities.put("&#61;","=");
	    htmlEntities.put("&gt;",">");
	    htmlEntities.put("&#62;",">");
	    htmlEntities.put("&#63;","?");
	    htmlEntities.put("&#64;","@");
	    htmlEntities.put("&#91;","[");
	    htmlEntities.put("&#92;","\\");
	    htmlEntities.put("&#93;","]");
	    htmlEntities.put("&#94;","^");
	    htmlEntities.put("&#95;","_");
	    htmlEntities.put("&#96;","`");
	    htmlEntities.put("&#123;","{");
	    htmlEntities.put("&#124;","|");
	    htmlEntities.put("&#125;","}");
	    htmlEntities.put("&#126;","~");
	    htmlEntities.put("&hellip;","…");
	    htmlEntities.put("&ndash;","–");
	    htmlEntities.put("&mdash;","—");
	    htmlEntities.put("&nbsp;","");
	    htmlEntities.put("&iexcl;","¡");
	    htmlEntities.put("&cent;","¢");
	    htmlEntities.put("&pound;","£");
	    htmlEntities.put("&curren;","¤");
	    htmlEntities.put("&yen;","¥");
	    htmlEntities.put("&brvbar; or &brkbar;","¦");
	    htmlEntities.put("&sect;","§");
	    htmlEntities.put("&uml; or &die;","¨");
	    htmlEntities.put("&copy;","©");
	    htmlEntities.put("&ordf;","ª");
	    htmlEntities.put("&laquo;","«");
	    htmlEntities.put("&not;","¬");
	    htmlEntities.put("&shy;","");
	    htmlEntities.put("&reg;","®");
	    htmlEntities.put("&macr; or &hibar;","¯");
	    htmlEntities.put("&deg;","°");
	    htmlEntities.put("&plusmn;","±");
	    htmlEntities.put("&sup2;","²");
	    htmlEntities.put("&sup3;","³");
	    htmlEntities.put("&acute;","´");
	    htmlEntities.put("&micro;","µ");
	    htmlEntities.put("&para;","¶");
	    htmlEntities.put("&middot;","·");
	    htmlEntities.put("&cedil;","¸");
	    htmlEntities.put("&sup1;","¹");
	    htmlEntities.put("&ordm;","º");
	    htmlEntities.put("&raquo;","»");
	    htmlEntities.put("&frac14;","¼");
	    htmlEntities.put("&frac12;","½");
	    htmlEntities.put("&frac34;","¾");
	    htmlEntities.put("&iquest;","¿");
	    htmlEntities.put("&Agrave;","À");
	    htmlEntities.put("&Aacute;","Á");
	    htmlEntities.put("&Acirc;","Â");
	    htmlEntities.put("&Atilde;","Ã");
	    htmlEntities.put("&Auml;","Ä");
	    htmlEntities.put("&Aring;","Å");
	    htmlEntities.put("&AElig;","Æ");
	    htmlEntities.put("&Ccedil;","Ç");
	    htmlEntities.put("&Egrave;","È");
	    htmlEntities.put("&Eacute;","É");
	    htmlEntities.put("&Ecirc;","Ê");
	    htmlEntities.put("&Euml;","Ë");
	    htmlEntities.put("&Igrave;","Ì");
	    htmlEntities.put("&Iacute;","Í");
	    htmlEntities.put("&Icirc;","Î");
	    htmlEntities.put("&Iuml;","Ï");
	    htmlEntities.put("&ETH;","Ð");
	    htmlEntities.put("&Ntilde;","Ñ");
	    htmlEntities.put("&Ograve;","Ò");
	    htmlEntities.put("&Oacute;","Ó");
	    htmlEntities.put("&Ocirc;","Ô");
	    htmlEntities.put("&Otilde;","Õ");
	    htmlEntities.put("&Ouml;","Ö");
	    htmlEntities.put("&times;","×");
	    htmlEntities.put("&Oslash;","Ø");
	    htmlEntities.put("&Ugrave;","Ù");
	    htmlEntities.put("&Uacute;","Ú");
	    htmlEntities.put("&Ucirc;","Û");
	    htmlEntities.put("&Uuml;","Ü");
	    htmlEntities.put("&Yacute;","Ý");
	    htmlEntities.put("&THORN;","Þ");
	    htmlEntities.put("&szlig;","ß");
	    htmlEntities.put("&agrave;","à");
	    htmlEntities.put("&aacute;","á");
	    htmlEntities.put("&acirc;","â");
	    htmlEntities.put("&atilde;","ã");
	    htmlEntities.put("&auml;","ä");
	    htmlEntities.put("&aring;","å");
	    htmlEntities.put("&aelig;","æ");
	    htmlEntities.put("&ccedil;","ç");
	    htmlEntities.put("&egrave;","è");
	    htmlEntities.put("&eacute;","é");
	    htmlEntities.put("&ecirc;","ê");
	    htmlEntities.put("&euml;","ë");
	    htmlEntities.put("&igrave;","ì");
	    htmlEntities.put("&iacute;","í");
	    htmlEntities.put("&icirc;","î");
	    htmlEntities.put("&iuml;","ï");
	    htmlEntities.put("&eth;","ð");
	    htmlEntities.put("&ntilde;","ñ");
	    htmlEntities.put("&ograve;","ò");
	    htmlEntities.put("&oacute;","ó");
	    htmlEntities.put("&ocirc;","ô");
	    htmlEntities.put("&otilde;","õ");
	    htmlEntities.put("&ouml;","ö");
	    htmlEntities.put("&divide;","÷");
	    htmlEntities.put("&oslash;","ø");
	    htmlEntities.put("&ugrave;","ù");
	    htmlEntities.put("&uacute;","ú");
	    htmlEntities.put("&ucirc;","û");
	    htmlEntities.put("&uuml;","ü");
	    htmlEntities.put("&yacute;","ý");
	    htmlEntities.put("&thorn;","þ");
	    htmlEntities.put("&yuml;","ÿ");
	    htmlEntities.put("&#133;","…");
	    htmlEntities.put("&#150;","–");
	    htmlEntities.put("&#151;","—");
	    htmlEntities.put("&#160;","");
	    htmlEntities.put("&#161;","¡");
	    htmlEntities.put("&#162;","¢");
	    htmlEntities.put("&#163;","£");
	    htmlEntities.put("&#164;","¤");
	    htmlEntities.put("&#165;","¥");
	    htmlEntities.put("&#166;","¦");
	    htmlEntities.put("&#167;","§");
	    htmlEntities.put("&#168;","¨");
	    htmlEntities.put("&#169;","©");
	    htmlEntities.put("&#170;","ª");
	    htmlEntities.put("&#171;","«");
	    htmlEntities.put("&#172;","¬");
	    htmlEntities.put("&#173;","");
	    htmlEntities.put("&#174;","®");
	    htmlEntities.put("&#175;","¯");
	    htmlEntities.put("&#176;","°");
	    htmlEntities.put("&#177;","±");
	    htmlEntities.put("&#178;","²");
	    htmlEntities.put("&#179;","³");
	    htmlEntities.put("&#180;","´");
	    htmlEntities.put("&#181;","µ");
	    htmlEntities.put("&#182;","¶");
	    htmlEntities.put("&#183;","·");
	    htmlEntities.put("&#184;","¸");
	    htmlEntities.put("&#185;","¹");
	    htmlEntities.put("&#186;","º");
	    htmlEntities.put("&#187;","»");
	    htmlEntities.put("&#188;","¼");
	    htmlEntities.put("&#189;","½");
	    htmlEntities.put("&#190;","¾");
	    htmlEntities.put("&#191;","¿");
	    htmlEntities.put("&#192;","À");
	    htmlEntities.put("&#193;","Á");
	    htmlEntities.put("&#194;","Â");
	    htmlEntities.put("&#195;","Ã");
	    htmlEntities.put("&#196;","Ä");
	    htmlEntities.put("&#197;","Å");
	    htmlEntities.put("&#198;","Æ");
	    htmlEntities.put("&#199;","Ç");
	    htmlEntities.put("&#200;","È");
	    htmlEntities.put("&#201;","É");
	    htmlEntities.put("&#202;","Ê");
	    htmlEntities.put("&#203;","Ë");
	    htmlEntities.put("&#204;","Ì");
	    htmlEntities.put("&#205;","Í");
	    htmlEntities.put("&#206;","Î");
	    htmlEntities.put("&#207;","Ï");
	    htmlEntities.put("&#208;","Ð");
	    htmlEntities.put("&#209;","Ñ");
	    htmlEntities.put("&#210;","Ò");
	    htmlEntities.put("&#211;","Ó");
	    htmlEntities.put("&#212;","Ô");
	    htmlEntities.put("&#213;","Õ");
	    htmlEntities.put("&#214;","Ö");
	    htmlEntities.put("&#215;","×");
	    htmlEntities.put("&#216;","Ø");
	    htmlEntities.put("&#217;","Ù");
	    htmlEntities.put("&#218;","Ú");
	    htmlEntities.put("&#219;","Û");
	    htmlEntities.put("&#220;","Ü");
	    htmlEntities.put("&#221;","Ý");
	    htmlEntities.put("&#222;","Þ");
	    htmlEntities.put("&#223;","ß");
	    htmlEntities.put("&#224;","à");
	    htmlEntities.put("&#225;","á");
	    htmlEntities.put("&#226;","â");
	    htmlEntities.put("&#227;","ã");
	    htmlEntities.put("&#228;","ä");
	    htmlEntities.put("&#229;","å");
	    htmlEntities.put("&#230;","æ");
	    htmlEntities.put("&#231;","ç");
	    htmlEntities.put("&#232;","è");
	    htmlEntities.put("&#233;","é");
	    htmlEntities.put("&#234;","ê");
	    htmlEntities.put("&#235;","ë");
	    htmlEntities.put("&#236;","ì");
	    htmlEntities.put("&#237;","í");
	    htmlEntities.put("&#238;","î");
	    htmlEntities.put("&#239;","ï");
	    htmlEntities.put("&#240;","ð");
	    htmlEntities.put("&#241;","ñ");
	    htmlEntities.put("&#242;","ò");
	    htmlEntities.put("&#243;","ó");
	    htmlEntities.put("&#244;","ô");
	    htmlEntities.put("&#245;","õ");
	    htmlEntities.put("&#246;","ö");
	    htmlEntities.put("&#247;","÷");
	    htmlEntities.put("&#248;","ø");
	    htmlEntities.put("&#249;","ù");
	    htmlEntities.put("&#250;","ú");
	    htmlEntities.put("&#251;","û");
	    htmlEntities.put("&#252;","ü");
	    htmlEntities.put("&#253;","ý");
	    htmlEntities.put("&#254;","þ");
	    htmlEntities.put("&#255;","ÿ");
	    htmlEntities.put("&Alpha;","Α");
	    htmlEntities.put("&alpha;","α");
	    htmlEntities.put("&Beta;","Β");
	    htmlEntities.put("&beta;","β");
	    htmlEntities.put("&Gamma;","Γ");
	    htmlEntities.put("&gamma;","γ");
	    htmlEntities.put("&Delta;","Δ");
	    htmlEntities.put("&delta;","δ");
	    htmlEntities.put("&Epsilon;","Ε");
	    htmlEntities.put("&epsilon;","ε");
	    htmlEntities.put("&Zeta;","Ζ");
	    htmlEntities.put("&zeta;","ζ");
	    htmlEntities.put("&Eta;","Η");
	    htmlEntities.put("&eta;","η");
	    htmlEntities.put("&Theta;","Θ");
	    htmlEntities.put("&theta;","θ");
	    htmlEntities.put("&Iota;","Ι");
	    htmlEntities.put("&iota;","ι");
	    htmlEntities.put("&Kappa;","Κ");
	    htmlEntities.put("&kappa;","κ");
	    htmlEntities.put("&Lambda;","Λ");
	    htmlEntities.put("&lambda;","λ");
	    htmlEntities.put("&Mu;","Μ");
	    htmlEntities.put("&mu;","μ");
	    htmlEntities.put("&Nu;","Ν");
	    htmlEntities.put("&nu;","ν");
	    htmlEntities.put("&Xi;","Ξ");
	    htmlEntities.put("&xi;","ξ");
	    htmlEntities.put("&Omicron;","Ο");
	    htmlEntities.put("&omicron;","ο");
	    htmlEntities.put("&Pi;","Π");
	    htmlEntities.put("&pi;","π");
	    htmlEntities.put("&Rho;","Ρ");
	    htmlEntities.put("&rho;","ρ");
	    htmlEntities.put("&Sigma;","Σ");
	    htmlEntities.put("&sigma;","σ");
	    htmlEntities.put("&Tau;","Τ");
	    htmlEntities.put("&tau;","τ");
	    htmlEntities.put("&Upsilon;","Υ");
	    htmlEntities.put("&upsilon;","υ");
	    htmlEntities.put("&Phi;","Φ");
	    htmlEntities.put("&phi;","φ");
	    htmlEntities.put("&Chi;","Χ");
	    htmlEntities.put("&chi;","χ");
	    htmlEntities.put("&Psi;","Ψ");
	    htmlEntities.put("&psi;","ψ");
	    htmlEntities.put("&Omega;","Ω");
	    htmlEntities.put("&omega;","ω");
	    htmlEntities.put("&#9679;","●");
	    htmlEntities.put("&#8212;"," ");
	    htmlEntities.put("&#8226;","•");
	    htmlEntities.put("&#8230;"," ");
	    htmlEntities.put("&#8243;"," ");
	  }

	  
	public RssItem(String providerVocalizing, String sender, String senderBmpURL, String title, String description, Date pubDate, String link, String bmpURL, String lang) {
		this.sender = sender;
		this.senderBmpURL=senderBmpURL;
		this.bmpURL=bmpURL;
		this.title = title;
		this.description = description;
		this.pubDate = pubDate;
		this.link = link;
		this.providerVocalizing=providerVocalizing;
		this.lang=lang;
	}
  //
	public String getSender() {
		return sender;
	}
	public String getSenderBmpURL() {
		return senderBmpURL;
	}
	public String getBmpURL(){
		return bmpURL;
	}

	public String getTitle() {
		return this.title;
	}
  
	public String getLink() {
		return this.link;
	}
  
	public String getDescription() {
		return this.description;
	}
	
	public Date getPubDate() {
		return this.pubDate;
	}

	public String getLang() {
		return this.lang;
	}

	static public String removeXml(String str) {
		int sz = str.length();
		StringBuffer buffer = new StringBuffer(sz);
		boolean inTag = false;
		for(int i=0; i<sz; i++) {
			char ch = str.charAt(i);
				if(ch == '<') {
					inTag = true;
				} else {
					if(ch == '>') {
						inTag = false;
						continue;
					}
				}
				if(!inTag) {
					buffer.append(ch);
				}
		}
		return buffer.toString();
	}

	public static final String unescapeHTML(String source, int start){
	     int i,j;
	     i = source.indexOf("&", start);
	     if (i > -1) {
	        j = source.indexOf(";" ,i);
	        if (j > i) {
	           String entityToLookFor = source.substring(i , j + 1);
	           String value = (String)htmlEntities.get(entityToLookFor);
	           if (value != null) {
	             source = new StringBuffer().append(source.substring(0 , i)).append(" ").append(source.substring(j + 1)).toString();
	           }
	           return unescapeHTML(source, i + 1); // recursive call
	         }
	     }
	     return source;
	  }
	
	public static Spanned removeImageSpanObjects(String inStr) {
	    SpannableStringBuilder spannedStr = (SpannableStringBuilder) Html.fromHtml(inStr.trim());
	    Object[] spannedObjects = spannedStr.getSpans(0, spannedStr.length(), Object.class);
	    for (int i = 0; i < spannedObjects.length; i++) {
	        if (spannedObjects[i] instanceof ImageSpan) {
	            spannedStr.replace(spannedStr.getSpanStart(spannedObjects[i]), spannedStr.getSpanEnd(spannedObjects[i]), "");
	        }
	    }
	    return spannedStr;
	}
	
	public static String getTextFromElement(Element e) {
		String res="";
	    //Node child = e.getFirstChild();
	    //if (child!=null) {
		//    if (child instanceof CharacterData) {
		//      CharacterData cd = (CharacterData) child;
		//      res=cd.getData();
		//      Log.e("[---!!!---]", "! "+res);
		//    }
	    //}
	    if (res==null||res.isEmpty()) res=e.getTextContent();
	    if (res==null) res="";
	    //Log.e("[---!!!---]", "! "+res);
	    return res;
	}
	
	public static ArrayList<RssItem> getRssItems(String feedUrl, String providerVocalizing, String lang, String provider, String category) {
		ArrayList<RssItem> rssItems = new ArrayList<RssItem>();
		try {
			URL url = new URL(feedUrl);
			HttpURLConnection.setFollowRedirects(true);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			 
			int status = conn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
					String newUrl = conn.getHeaderField("Location");
					String cookies = conn.getHeaderField("Set-Cookie");
					conn = (HttpURLConnection) new URL(newUrl).openConnection();
					conn.setRequestProperty("Cookie", cookies);
				}
			}
			
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document document = db.parse(is);
				Element element = document.getDocumentElement();
				Element _channelE = (Element) element.getElementsByTagName("channel").item(0);
				String _sender="";
				String _senderBitmapURL="";
				Date lastBuildDate=new Date();
				if (_channelE!=null) {
					Element _senderE = (Element) _channelE.getElementsByTagName("title").item(0);
					if (_senderE!=null) _sender=getTextFromElement(_senderE);
					Element _senderImageE = (Element) _channelE.getElementsByTagName("image").item(0);
					if (_senderImageE!=null) {
						Element _imageURLE = (Element) _senderImageE.getElementsByTagName("url").item(0);
						if (_imageURLE!=null) {
							_senderBitmapURL=getTextFromElement(_imageURLE);
						}
					}
					Element _buildDateE = (Element) _channelE.getElementsByTagName("lastBuildDate").item(0);
					if (_buildDateE!=null&&!getTextFromElement(_buildDateE).equals(""))  {
						lastBuildDate=new Date(getTextFromElement(_buildDateE));
					}
				} else {
					Element _authorE = (Element) element.getElementsByTagName("author").item(0);
					if (_authorE!=null) {
						Element _senderE = (Element) _authorE.getElementsByTagName("name").item(0);
						if (_senderE!=null) _sender=getTextFromElement(_senderE);						
					}
					Element _senderImageE = (Element) element.getElementsByTagName("logo").item(0);
					if (_senderImageE!=null) {
						_senderBitmapURL=getTextFromElement(_senderImageE);
					}

				}
				//take rss nodes to NodeList
				NodeList nodeList = element.getElementsByTagName("item");
				if (nodeList.getLength() == 0)
					nodeList = element.getElementsByTagName("entry");
				if (nodeList.getLength() > 0) {
					for (int i = 0; i < nodeList.getLength(); i++) {						
						Element entry = (Element) nodeList.item(i);
						Element _titleE = (Element) entry.getElementsByTagName("title").item(0);
						Element _descriptionE = (Element) entry.getElementsByTagName("description").item(0);
						Element _pubDateE = (Element) entry.getElementsByTagName("pubDate").item(0);
						if (_pubDateE==null)
							_pubDateE = (Element) entry.getElementsByTagName("updated").item(0);
						if (_pubDateE==null)
							_pubDateE = (Element) entry.getElementsByTagName("published").item(0);
						Element _linkE = (Element) entry.getElementsByTagName("link").item(1);
						if (_linkE==null) _linkE = (Element) entry.getElementsByTagName("link").item(0);
						Element _enclosureE = (Element) entry.getElementsByTagName("enclosure").item(0);
						Element _contentE = (Element) entry.getElementsByTagName("content:encoded").item(0);
						if (_contentE==null) {
							_contentE = (Element) entry.getElementsByTagName("content").item(0);
						}
						Element _summaryE = (Element) entry.getElementsByTagName("summary").item(0);
						Element _thumbnailE = (Element) entry.getElementsByTagName("media:thumbnail").item(1);
						if (_thumbnailE==null) _thumbnailE = (Element) entry.getElementsByTagName("media:thumbnail").item(0);
						
						String _title = "";
						if (_titleE!=null) _title = getTextFromElement(_titleE);
						
						String _description = "";
						if (_descriptionE!=null) _description = getTextFromElement(_descriptionE);
						
						Date _pubDate = new Date();
						if (_pubDateE!=null) {
							
							final String date = getTextFromElement(_pubDateE);							
							try {
								_pubDate = new Date(date);
							} catch (Exception e) {
								final String pattern = "yyyy-MM-dd'T'hh:mm:ss";
								final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
								_pubDate = sdf.parse(date);
							}
						}
						if ((lastBuildDate.getTime()-_pubDate.getTime())<0) {
							_pubDate=lastBuildDate;
						}
						
						String _link = "";
						if (_linkE!=null) _link = _linkE.getAttribute("href");
						if (_link.equals("")) if (_linkE!=null)  _link = getTextFromElement(_linkE);						
						
						String _imgLink="";
						if (_enclosureE!=null) _imgLink=_enclosureE.getAttribute("url");
						if (_imgLink.equals(""))
							if (_thumbnailE!=null)
								_imgLink=_thumbnailE.getAttribute("url");
						
						String _content = "";
						if (_contentE!=null) _content = getTextFromElement(_contentE);
						if (!_content.equals("")) _description=_content;
						if (_description.equals(""))
							if (_summaryE!=null)
								_description=getTextFromElement(_summaryE);
									
						_description = ""+removeImageSpanObjects(_description);
						_title=_title.replace("\\\\", ".");
						_title=_title.replace("//", ".");
						_description=_description.replace("\\\\", ".");
						_description=_description.replace("//", ".");
						_description=_description.replace("\\", "");
						_description=_description.replace("\"Ъ\"", "Коммерсант");
						_title=_title.replace("\"Ъ\"", "Коммерсант");
						if (_sender.equals("")) _sender=provider+((category.equals(""))? "" : (":"+category));
						if (lang.equals("auto")) {
							lang = SpeindAPI.getLang(_title+" "+_description);
						}
						RssItem rssItem = new RssItem(providerVocalizing, _sender, _senderBitmapURL, _title, _description, _pubDate, _link, _imgLink, lang);
						rssItems.add(rssItem);
					}
				}
				is.close();
				conn.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return rssItems;
	}
}