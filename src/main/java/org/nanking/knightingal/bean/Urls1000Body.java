package org.nanking.knightingal.bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class Urls1000Body {

    public Urls1000Body() {
    }

    public Urls1000Body(String title, List<ImgSrcBean> imgSrcArray) {
        this.title = title;
        this.imgSrcArray = imgSrcArray;
    }

    private String title;

    private List<ImgSrcBean> imgSrcArray;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ImgSrcBean> getImgSrcArray() {
        return imgSrcArray;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public void setImgSrcArray(List<ImgSrcBean> imgSrcArray) {
        this.imgSrcArray = imgSrcArray;
    }

    public static class ImgSrcBean {
        public ImgSrcBean() {
        }

        public ImgSrcBean(String src, String ref) {
            this.src = src;
            this.ref = ref;
        }

        private String src;

        private String ref;

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }
    }
}
