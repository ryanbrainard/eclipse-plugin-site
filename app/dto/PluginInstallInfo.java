package dto;

public class PluginInstallInfo {

	private Long installDt;
	private String requestorIP;
	private String userAgent;
	public PluginInstallInfo(){
		super();
	}
	public PluginInstallInfo(Long installDt, String ipAddr,String userAgent) {
		this();
		this.installDt = installDt;
		this.requestorIP = ipAddr;
		this.userAgent=userAgent;
	}
	public Long getInstallDt() {
		return installDt;
	}
	public void setInstallDt(Long installDt) {
		this.installDt = installDt;
	}
	public String getRequestorIP() {
		return requestorIP;
	}
	public void setRequestorIP(String hostName) {
		this.requestorIP = hostName;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	@Override
	public String toString() {
		return "PluginInstallInfo [installDt=" + installDt + ", hostName="
				+ requestorIP + "]";
	}
	
}
