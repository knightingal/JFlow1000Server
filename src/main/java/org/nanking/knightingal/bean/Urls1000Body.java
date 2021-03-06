package org.nanking.knightingal.bean;

import java.util.List;

/**
 * @author Knightingal
 */
public class Urls1000Body {

    public Urls1000Body() {
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
        return "Urls1000Body{" +
                "title='" + title + '\'' +
                ", imgSrcArray=" + imgSrcArray +
                '}';
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

        @Override
        public String toString() {
            return "ImgSrcBean{" +
                    "src='" + src + '\'' +
                    ", ref='" + ref + '\'' +
                    '}';
        }
    }
}
