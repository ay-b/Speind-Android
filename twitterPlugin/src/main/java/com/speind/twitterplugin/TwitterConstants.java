package com.speind.twitterplugin;

public class TwitterConstants {
	//public static final String CONSUMER_KEY = "ndvsZC97jKp7wiEMoCFY0NWRT";
	//public static final String CONSUMER_SECRET= "H7qIfa5xKC0RfwVJWeqJq1BkO6f6eW2ZBD5Cjt2fwDcXlZ1kMX";

	public static final String CONSUMER_KEY = "pYqTEFyReNUZs5Gri6scPJNpE";
	public static final String CONSUMER_SECRET= "EQzjRRv5omeAe1sihiMTA6ZvcTS3CnEVtcsyDqvpeR9PkF9Kc7";
	
	public static final String REQUEST_URL		= "https://api.twitter.com/oauth/request_token";
	public static final String ACCESS_URL		= "https://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL	= "https://api.twitter.com/oauth/authorize";

	public static final String OAUTH_CALLBACK_SCHEME	= "appfortwitter";
	public static final String OAUTH_CALLBACK_HOST	= "callback";
	public static final String OAUTH_CALLBACK_URL	=  OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
}
