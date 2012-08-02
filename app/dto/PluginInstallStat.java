package dto;

public class PluginInstallStat implements Comparable<PluginInstallStat> {

    private String installDt;
    private Integer installCount;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((installDt == null) ? 0 : installDt.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PluginInstallStat other = (PluginInstallStat) obj;
        if (installDt == null) {
            if (other.installDt != null) {
                return false;
            }
        } else if (!installDt.equals(other.installDt)) {
            return false;
        }
        return true;
    }

    public PluginInstallStat(String installDt, Integer installCount) {
        super();
        this.installDt = installDt;
        this.installCount = installCount;
    }

    public String getInstallDt() {
        return installDt;
    }

    public void setInstallDt(String installDt) {
        this.installDt = installDt;
    }

    public Integer getInstallCount() {
        return installCount;
    }

    public void setInstallCount(Integer installCount) {
        this.installCount = installCount;
    }

    public void incrementCount() {
        this.installCount++;
    }

    @Override
    public String toString() {
        return "PluginInstallStat [installDt=" + installDt + ", installCount="
                + installCount + "]";
    }

    @Override
    public int compareTo(PluginInstallStat o) {
        return this.installDt.compareTo(o.getInstallDt());
    }
}
