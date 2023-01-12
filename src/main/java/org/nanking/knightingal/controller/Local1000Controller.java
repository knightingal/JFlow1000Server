package org.nanking.knightingal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nanking.knightingal.bean.*;
import org.nanking.knightingal.dao.Local1000ImgDao;
import org.nanking.knightingal.dao.Local1000SectionDao;
import org.nanking.knightingal.runnable.DownloadImgRunnable;
import org.nanking.knightingal.service.WsMsgService;
import org.nanking.knightingal.util.FileUtil;
import org.nanking.knightingal.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;


/**
 * @author Knightingal
 */
@RequestMapping("/local1000")
@RestController
public class Local1000Controller {

    private static final Log log = LogFactory.getLog(Local1000Controller.class);

    @Autowired
    private Local1000SectionDao local1000SectionDao;

    @Autowired
    private Local1000ImgDao local1000ImgDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Executor downloadImgThreadPoolExecutor;

    @Autowired
    private Executor downloadSectionThreadPoolExecutor;

    @Autowired
    private WsMsgService wsMsgService;

    @Value("${baseDir}")
    private String baseDir;

    @RequestMapping("/picDetailAjax")
    public SectionDetail picDetailAjax(@RequestParam(value = "id", defaultValue = "1") int id) {
        log.info("handle /picDetailAjax, id=" + id);
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        List<Flow1000Img> flow1000ImgList = local1000ImgDao.queryFlow1000ImgBySectionId(id);
        List<ImgDetail> imgDetailList = new ArrayList<>();
        for (Flow1000Img flow1000Img : flow1000ImgList) {
            imgDetailList.add(new ImgDetail(
                    flow1000Img.getName(),
                    flow1000Img.getWidth(),
                    flow1000Img.getHeight())
            );
        }

        return new SectionDetail(flow1000Section.getDirName(), flow1000Section.getId(), imgDetailList);
    }

    @RequestMapping("/picContentAjax")
    public SectionContent picContentAjax(@RequestParam(value = "id", defaultValue = "1") int id) {
        log.info("handle /picDetailAjax, id=" + id);
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        List<Flow1000Img> flow1000ImgList = local1000ImgDao.queryFlow1000ImgBySectionId(id);
        List<String> imgList = new ArrayList<>();
        for (Flow1000Img flow1000Img : flow1000ImgList) {
            imgList.add(flow1000Img.getName());
        }

        return new SectionContent(flow1000Section.getDirName(), flow1000Section.getId(), imgList);
    }

    // @RequestMapping("/searchSection")
    // public List<PicIndex> searchSection(@RequestParam(value="name", defaultValue="")String name) {
    //     log.debug("searchSection request, name=" + name);
    //     if ("".equals(name)) {
    //         return new ArrayList<PicIndex>();
    //     }
    //     name = "%" + name + "%";
    //     List<Flow1000Section> searchResult = local1000SectionDao.searchFlow1000SectionByName(name);        

    //     return searchResult.stream().map(flow1000Section -> new PicIndex(
    //             flow1000Section.getId(),
    //             flow1000Section.getDirName(),
    //             flow1000Section.getCreateTime(),
    //             flow1000Section.getCover(),
    //             flow1000Section.getCoverWidth(),
    //             flow1000Section.getCoverHeight()
    //     )).collect(Collectors.toList());
    // }

    @RequestMapping("/picIndexAjax")
    public List<PicIndex> picIndexAjax(
            @RequestParam(value="time_stamp", defaultValue="19700101000000")String timeStamp,
            @RequestParam(value="album", defaultValue="")String album
    ) {
        log.info("handle /picIndexAjax, timeStamp=" + timeStamp);
        List<Flow1000Section> flow1000SectionList = local1000SectionDao.findAll(
            (Specification<Flow1000Section>) (root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (album != null && album.length() != 0) {
                    Predicate albumPredicate = builder.equal(root.get("album"), album);
                    predicates.add(albumPredicate);
                }

                Predicate createTimePredicate = builder.greaterThan(root.get("createTime"), timeStamp);
                predicates.add(createTimePredicate);
                return builder.and(predicates.toArray(new Predicate[] {}));
            }
        );

        return flow1000SectionList.stream().map(flow1000Section -> {
            return new PicIndex(
                    flow1000Section.getId(),
                    flow1000Section.getDirName(),
                    flow1000Section.getCreateTime(),
                    flow1000Section.getCover(),
                    flow1000Section.getCoverWidth(),
                    flow1000Section.getCoverHeight()
            );
        }).collect(Collectors.toList());
    }

    @RequestMapping("/picIndexAjaxByPage")
    public PicIndexPage picIndexAjaxByPage(
            @RequestParam(value="time_stamp", defaultValue="19700101000000")String timeStamp
    ) {
        log.info("handle /picIndexAjax, timeStamp=" + timeStamp);
        List<Flow1000Section> flow1000SectionList = local1000SectionDao.queryFlow1000SectionByCreateTime(timeStamp);
        List<PicIndex> picIndexList = new ArrayList<>();

        for (Flow1000Section flow1000Section : flow1000SectionList) {
            picIndexList.add(new PicIndex(
                    flow1000Section.getId(),
                    flow1000Section.getDirName(),
                    flow1000Section.getCreateTime(),
                    flow1000Section.getCover(),
                    flow1000Section.getCoverWidth(),
                    flow1000Section.getCoverHeight()
            ));
        }

        return new PicIndexPage(picIndexList.subList(0, 10), picIndexList.size());
    }

    @RequestMapping(value="/urls1000", method={RequestMethod.POST})
    public void urls1000(@RequestBody Urls1000Body urls1000Body) {
        log.info("handle /urls1000, body=" + urls1000Body.toString());
        String timeStamp = ((TimeUtil) applicationContext.getBean("timeUtil")).timeStamp();
        FileUtil fileUtil = (FileUtil) applicationContext.getBean("fileUtil");
        String dirName = timeStamp + urls1000Body.getTitle();
        String absPath = "/home/knightingal/download/linux1000/source/" + dirName + "/";
        File dirFile = new File(absPath);
        if (!dirFile.mkdirs()) {
            return;
        }

        Flow1000Section flow1000Section = new Flow1000Section();
        flow1000Section.setName(urls1000Body.getTitle());
        flow1000Section.setDirName(dirName);
        flow1000Section.setCreateTime(timeStamp);
        flow1000Section.setCover(
                fileUtil.getFileNameByUrl(urls1000Body.getImgSrcArray().get(0).getSrc())
        );
        flow1000Section.setAlbum("flow1000");
        List<Flow1000Img> flow1000ImgList = new ArrayList<>();
        for (Urls1000Body.ImgSrcBean imgSrcBean : urls1000Body.getImgSrcArray()) {
            String fileName = fileUtil.getFileNameByUrl(imgSrcBean.getSrc());
            Flow1000Img flow1000Img = new Flow1000Img();
            flow1000Img.setName(fileName);
            flow1000Img.setInCover(
                    urls1000Body.getImgSrcArray().lastIndexOf(imgSrcBean) == 0 ? 1 : 0
            );
            flow1000Img.setSectionId(flow1000Section.getId());
            flow1000Img.setSrc(imgSrcBean.getSrc());
            flow1000Img.setHref(imgSrcBean.getRef());
            flow1000ImgList.add(flow1000Img);
        }
        downloadSectionThreadPoolExecutor.execute(() -> {
            CountDownLatch countDownLatch = new CountDownLatch(flow1000ImgList.size());

            for (Flow1000Img flow1000Img : flow1000ImgList) {
                downloadImgThreadPoolExecutor.execute(new DownloadImgRunnable(
                        flow1000Img, dirName, countDownLatch, baseDir
                ));
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info(flow1000Section.getDirName() + " download complete");
            // local1000SectionDao.insertFlow1000Section(flow1000Section);
            flow1000ImgList.forEach(flow1000Img -> {
                flow1000Img.setSectionId(flow1000Section.getId());
            });
            local1000ImgDao.insertFlow1000Img(flow1000ImgList);
            PicIndex picIndex = new PicIndex(
                    flow1000Section.getId(),
                    flow1000Section.getDirName(),
                    flow1000Section.getCreateTime(),
                    flow1000Section.getCover(),
                    flow1000Section.getCoverWidth(),
                    flow1000Section.getCoverHeight()
            );
            ObjectMapper mapper = new ObjectMapper();
            try {
                wsMsgService.sendWsMsg(mapper.writeValueAsString(picIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @RequestMapping(value="/deleteSection", method={RequestMethod.POST})
    public void deleteSection(@RequestBody SectionDetail sectionDetail) {
        log.info("handle /deleteSection, sectionDetail=" + sectionDetail.toString());
        if (sectionDetail.getId() == null || sectionDetail.getId() <= 0) {
            return;
        }
        local1000ImgDao.deleteFlow1000ImgBySectionId(sectionDetail.getId());
        local1000SectionDao.deleteById(sectionDetail.getId());
    }
}
