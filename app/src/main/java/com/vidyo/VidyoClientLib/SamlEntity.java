package com.vidyo.VidyoClientLib;

import android.net.Uri;

import java.io.Serializable;


/**
 * The SamlEntity contains the values associated with the sign-in through SAML.
 * This class is used to pass values from the Java layer to JNI.
 */
public class SamlEntity implements Serializable {

    private static final String TAG = "SamlEntity";

    // vm info
    private String vmIdentity;
    private String serverAddress;
    private String serverPort;
    private boolean emcpSecured;

    private String pak;
    private String TlsProxy;
    private String un;
    private boolean showdialpad;
    private boolean showstartmeeting;
    private String portal;
    private String portalVersion;
    private String loctag;

    private String cookie;

    public boolean parseUrlHiddenTag(String code){

        Uri uri = Uri.parse(code);

        pak = uri.getQueryParameter("pak");
        TlsProxy = uri.getQueryParameter("TlsProxy");
        un = uri.getQueryParameter("un");
        showdialpad = uri.getQueryParameter("showdialpad").toLowerCase().equals("yes");
        showstartmeeting = uri.getQueryParameter("showstartmeeting").toLowerCase().equals("yes");
        portal = uri.getQueryParameter("portal");
        portalVersion = uri.getQueryParameter("portalVersion");
        loctag = uri.getQueryParameter("loctag");

        // parse vm info
        String vm = Uri.parse(uri.getQueryParameter("url")).getQuery();
        String[] vmArray = vm.split(";");
        for(String s: vmArray) {
            if(s.startsWith("vm=")) {
                s = s.replaceFirst("vm=", "");
                String[] sArray = s.split("@|:");
                if(sArray.length >= 3) {
                    vmIdentity = sArray[0];
                    serverAddress = sArray[1];
                    serverPort = sArray[2];
                }
            } else if (s.startsWith("transport=")) {
                s = s.replaceFirst("transport=", "");
                if(s.equalsIgnoreCase("TCP")) {
                    emcpSecured = false;
                } else if (s.equalsIgnoreCase("TLS")) {
                    emcpSecured = true;
                }
            }
        }

        return (vmIdentity != null) && (serverAddress != null) && (serverPort != null)
                && (pak != null) && (un != null) & (portal != null) & (portalVersion != null);
    }


    public String getVmIdentity() {return vmIdentity;}
    public String getServerAddress() { return serverAddress;}
    public String getServerPort() { return serverPort;}
    public boolean getEmcpSecured() {return emcpSecured;}
    public String getPak() {return pak;}
    public String getTlsProxy() {return TlsProxy;}
    public String getUn() {return un;}
    public boolean getShowdialpad() {return showdialpad;}
    public boolean getShowstartmeeting() {return showstartmeeting;}
    public String getPortal() {return portal;}
    public String getPortalVersion() {return portalVersion;}
    public String getLoctag() {return loctag;}
    public String getCookie() {return cookie;}

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

}
