package org.nanking.knightingal.bean;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Knightingal
 */
@Entity
public class Flow1000Img {

    public Flow1000Img() {
    }

    public Flow1000Img(int id, String name, Long sectionId, int inCover, int width, int height) {
        this.id = id;
        this.name = name;
        this.sectionId = sectionId;
        this.inCover = inCover;
        this.width = width;
        this.height = height;
    }

    @Id
    private int id;

    private String name;

    private Long sectionId;

    private int inCover;

    private int width;

    private int height;

    private String src;

    private String href;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public int getInCover() {
        return inCover;
    }

    public void setInCover(int inCover) {
        this.inCover = inCover;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
