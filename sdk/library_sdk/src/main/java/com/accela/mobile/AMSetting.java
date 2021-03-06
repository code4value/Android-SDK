/** 
  * Copyright 2014 Accela, Inc. 
  * 
  * You are hereby granted a non-exclusive, worldwide, royalty-free license to 
  * use, copy, modify, and distribute this software in source code or binary 
  * form for use in connection with the web services and APIs provided by 
  * Accela. 
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
  * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
  * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
  * DEALINGS IN THE SOFTWARE. 
  * 
  */
package com.accela.mobile;

import android.util.Log;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *  AccelaMobile Setting object, use to defines some global values such as SDK configurations, service URIs, and HTTP configurations.
 * 
 * @since 1.0
 */

public final class AMSetting {			
	/**
	 * SDK name and version, usually you needn't change it.
	 * 
	 * @since 1.0
	 */
	public static final String AM_SDK_NAME = "Accela-Mobile-SDK";
	public static final String AM_SDK_VERSION = "v4.1 2015-03-31";	
	
	/**
	 * The URL of AccelaMobile OAuth server and APIs cloud server.
	 * 
	 * @since 1.0
	 */	
	public static final String AM_OAUTH_HOST = "https://auth.accela.com";
	public static final String AM_API_HOST = "https://apis.accela.com";
	
	/**
	 * The URIs of services used to get authorization code and acess token.
	 * 
	 * @since 2.3
	 */	
	public static final String PUBLIC_AUTHORIZE_URI = "/oauth2/authorize";
	public static final String ACCESS_TOKEN_URI = "/oauth2/token";
	public static final String PRIVATE_AUTHORIZE_URI = "/sso/ticket";	
	
	/**
	 * The action names of broadcast messages.
	 * They are sent by SDK when user logs in successfully or access token becomes invalid .
	 * 
	 * @since 2.3
	 */
	public static final String BROARDCAST_ACTION_LOGGED_IN = "com.accela.mobile.action.LOGGED_IN";
	public static final String BROARDCAST_ACTION_SESSION_INVALID = "com.accela.mobile.action.SESSION_INVALID";	
	
	/**
	 * Timeout interval of estimating HTTP/HTTPS connection (unit: millisecond).
	 * Usually you needn't change the default value, but you are allowed to do so based on the real network speed.
	 * 
	 * @since 1.0
	 */
	public static final int HTTP_CONNECTION_TIMEOUT = 120 * 1000;	
	
	/**
	 * Timeout interval of waiting for data from HTTP/HTTPS connection (unit: millisecond).
	 * Usually you needn't change the default value, but you are allowed to do so based on the real network speed.
	 * 
	 * @since 1.0
	 */
	public static final int SOCKET_TIMEOUT = 120 * 1000;
	
	 /**
     * Debug flag for logging.
     * When it is set to true, more detailed debugging messages will be printed to log. 
     * Please set its value to true if you are working on the SDK source code,
     * and set its value to false before you build the SDK Jar package for a release.
     * 
     * @since 1.0
     */    
	public static Boolean DebugMode = false;  		
	
	
	/**
	 * Use the static method to get resource bundle for the current language set on the mobile device.
	 * Note: The file name will be localizable_en-US.properties for language en_US, and  localizable_ar-AE.properties for language ar_AE.
	 * 
	 * @since 2.3
	 */
	public static ResourceBundle getStringResourceBundle() {
		String bundleName = "com.accela.mobile.resources.localizable";
		ResourceBundle stringResourceBundle = null;
		try {
			stringResourceBundle = ResourceBundle.getBundle(bundleName);
		} catch (MissingResourceException e) {
			stringResourceBundle = ResourceBundle.getBundle(bundleName, Locale.US);
		} catch (ArithmeticException e) {
			AMLogger.logError(e.toString());
		}
		
		return stringResourceBundle;
	}
}
