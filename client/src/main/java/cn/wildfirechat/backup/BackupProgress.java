package cn.wildfirechat.backup;

/**
 * 备份恢复进度信息
 */
public class BackupProgress {
    private long totalUnitCount;
    private long completedUnitCount;
    private String currentPhase;
    private boolean isCancelled;

    public BackupProgress() {
        this.totalUnitCount = 0;
        this.completedUnitCount = 0;
        this.isCancelled = false;
    }

    public BackupProgress(long totalUnitCount) {
        this.totalUnitCount = totalUnitCount;
        this.completedUnitCount = 0;
        this.isCancelled = false;
    }

    public float getFractionCompleted() {
        if (totalUnitCount == 0) {
            return 0;
        }
        return (float) completedUnitCount / totalUnitCount;
    }

    public int getPercentage() {
        return (int) (getFractionCompleted() * 100);
    }

    public void increment() {
        completedUnitCount++;
    }

    public void increment(long delta) {
        completedUnitCount += delta;
    }

    // Getters and Setters
    public long getTotalUnitCount() {
        return totalUnitCount;
    }

    public void setTotalUnitCount(long totalUnitCount) {
        this.totalUnitCount = totalUnitCount;
    }

    public long getCompletedUnitCount() {
        return completedUnitCount;
    }

    public void setCompletedUnitCount(long completedUnitCount) {
        this.completedUnitCount = completedUnitCount;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public void cancel() {
        this.isCancelled = true;
    }
}
