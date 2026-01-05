package com.hk1089.mettax.video;

/**
 * Data model for video file search results
 * szFileInfo: szFile[256]:nYear:nMonth:nDay:uiBegintime:uiEndtime:szDevIDNO:nChn:nFileLen:nFileType:nLocation:nSvrID
 */
public class RecordFile {
    byte[] orginalFileInfo = new byte[1024];
    int orginalFileLen = 0;
    
    String devIdno;
    String fileInfo;
    String name;
    Integer year;
    Integer month;
    Integer day;
    Integer beginTime;
    Integer endTime;
    Integer chn;
    Integer fileLength;
    Integer fileType;
    Integer location;
    Integer svrId;
    Boolean isPlaying;
    
    Integer chnMask;
    Boolean stream;
    Boolean recording;
    Integer fileOffset;
    Integer alarmInfo;
    
    public String getDevIdno() {
        return devIdno;
    }
    
    public void setDevIdno(String devIdno) {
        this.devIdno = devIdno;
    }
    
    public String getFileInfo() {
        return fileInfo;
    }
    
    public void setFileInfo(String fileInfo) {
        this.fileInfo = fileInfo;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
    
    public Integer getMonth() {
        return month;
    }
    
    public void setMonth(Integer month) {
        this.month = month;
    }
    
    public Integer getDay() {
        return day;
    }
    
    public void setDay(Integer day) {
        this.day = day;
    }
    
    public Integer getBeginTime() {
        return beginTime;
    }
    
    public void setBeginTime(Integer beginTime) {
        this.beginTime = beginTime;
    }
    
    public Integer getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }
    
    public Integer getChn() {
        return chn;
    }
    
    public void setChn(Integer chn) {
        this.chn = chn;
    }
    
    public Integer getFileLength() {
        return fileLength;
    }
    
    public void setFileLength(Integer fileLength) {
        this.fileLength = fileLength;
    }
    
    public Integer getFileType() {
        return fileType;
    }
    
    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }
    
    public Integer getLocation() {
        return location;
    }
    
    public void setLocation(Integer location) {
        this.location = location;
    }
    
    public Integer getSvrId() {
        return svrId;
    }
    
    public void setSvrId(Integer svrId) {
        this.svrId = svrId;
    }
    
    public Boolean getIsPlaying() {
        return isPlaying;
    }
    
    public void setIsPlaying(Boolean isPlaying) {
        this.isPlaying = isPlaying;
    }
    
    public Integer getChnMask() {
        return chnMask;
    }
    
    public void setChnMask(Integer chnMask) {
        this.chnMask = chnMask;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public Boolean getRecording() {
        return recording;
    }
    
    public void setRecording(Boolean recording) {
        this.recording = recording;
    }
    
    public Integer getFileOffset() {
        return fileOffset;
    }
    
    public void setFileOffset(Integer fileOffset) {
        this.fileOffset = fileOffset;
    }
    
    public Integer getAlarmInfo() {
        return alarmInfo;
    }
    
    public void setAlarmInfo(Integer alarmInfo) {
        this.alarmInfo = alarmInfo;
    }
    
    public void setOrginalFileInfo(byte[] info, int length) {
        System.arraycopy(info, 0, orginalFileInfo, 0, length);
        orginalFileLen = length;
    }
    
    public byte[] getOrginalFile() {
        return orginalFileInfo;
    }
    
    public int getOrginalLen() {
        return orginalFileLen;
    }
    
    protected String formatSecond2Time(Integer time) {
        return String.format("%02d:%02d:%02d", time.intValue()/3600, time.intValue()%3600/60, time.intValue()%60);
    }
    
    public String getFileTime() {
        return String.format("%04d-%02d-%02d    %s - %s", getYear(), getMonth(), getDay()
                , formatSecond2Time(beginTime), formatSecond2Time(endTime));
    }
    
    public String getFileDate() {
        return String.format("%04d-%02d-%02d", getYear(), getMonth(), getDay());
    }
    
    public String getFileTimeEx() {
        return String.format("%s-%s", formatSecond2Time(beginTime), formatSecond2Time(endTime));
    }
}


