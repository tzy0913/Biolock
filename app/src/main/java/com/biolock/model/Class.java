package com.biolock.model;

public class Class {
    private long classId;
    private String moduleCode;
    private String moduleName;
    private String section;
    private String type;
    private long instructor;
    private String room;

    // Getters and setters
    public long getClassId() { return classId; }
    public void setClassId(long classId) { this.classId = classId; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getInstructor() { return instructor; }
    public void setInstructor(long instructor) { this.instructor = instructor; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
}