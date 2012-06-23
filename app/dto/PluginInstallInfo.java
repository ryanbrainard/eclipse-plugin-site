package dto;

public class PluginInstallInfo {

	private Long installDt;
	private String hostName;
	public PluginInstallInfo(Long installDt, String hostName) {
		super();
		this.installDt = installDt;
		this.hostName = hostName;
	}
	public Long getInstallDt() {
		return installDt;
	}
	public void setInstallDt(Long installDt) {
		this.installDt = installDt;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	@Override
	public String toString() {
		return "PluginInstallInfo [installDt=" + installDt + ", hostName="
				+ hostName + "]";
	}
	
}
