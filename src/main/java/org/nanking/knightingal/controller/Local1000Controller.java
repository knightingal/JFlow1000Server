package org.nanking.knightingal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nanking.knightingal.bean.*;
import org.nanking.knightingal.dao.Local1000AlbumConfigDao;
import org.nanking.knightingal.dao.Local1000ImgDao;
import org.nanking.knightingal.dao.Local1000SectionDao;
import org.nanking.knightingal.runnable.DownloadImgRunnable;
import org.nanking.knightingal.service.WsMsgService;
import org.nanking.knightingal.util.FileUtil;
import org.nanking.knightingal.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * @author Knightingal
 */
@RequestMapping("/local1000")
@RestController
public class Local1000Controller {

    private static final Log log = LogFactory.getLog(Local1000Controller.class);

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Local1000Controller(Local1000SectionDao local1000SectionDao, Local1000ImgDao local1000ImgDao, Local1000AlbumConfigDao local1000AlbumConfigDao) {
        this.local1000ImgDao = local1000ImgDao;
        this.local1000SectionDao = local1000SectionDao;
        this.local1000AlbumConfigDao = local1000AlbumConfigDao;
    }

    private final Local1000SectionDao local1000SectionDao;

    private final Local1000ImgDao local1000ImgDao;

    private final Local1000AlbumConfigDao local1000AlbumConfigDao;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Executor downloadImgThreadPoolExecutor;

    @Autowired
    private Executor downloadSectionThreadPoolExecutor;

    @Autowired
    private WsMsgService wsMsgService;

    private String baseDir;

    @RequestMapping("/init")
    public Object init() {
        File baseDirFile = new File(baseDir + "/source");
        // final String[] dirList = Arrays.copyOfRange(list, 0, 10);
        final String[] dirList = baseDirFile.list();

        executorService.submit(() -> {
            List<Flow1000Section> sectionList = Stream.of(dirList).map(dirName -> {
                log.error("process " + dirName);
                log.error("===========================");
                String timeStamp = dirName.substring(0, 14);
                String name = dirName.substring(14);
                Flow1000Section flow1000Section = new Flow1000Section();
                flow1000Section.setName(name);
                flow1000Section.setCreateTime(timeStamp);
                flow1000Section.setDirName(dirName);

                String[] imgNameArray = listImages(dirName);

                List<Flow1000Img> imgList = Stream.of(imgNameArray)
                        .sorted(Local1000Controller::compareImgName)
                        .map(imgNameItem -> generate1000Img(imgNameItem, flow1000Section))
                        .collect(Collectors.toList());
                flow1000Section.setImages(imgList);
                flow1000Section.setCover(imgList.get(0).getName());
                flow1000Section.setCoverHeight(imgList.get(0).getHeight());
                flow1000Section.setCoverWidth(imgList.get(0).getWidth());
                return flow1000Section;
            }).collect(Collectors.toList());

            local1000SectionDao.saveAllAndFlush(sectionList);

        });

        return null;
    }

    @RequestMapping("/picDetailAjax")
    public SectionDetail picDetailAjax(@RequestParam(value = "id", defaultValue = "1") Long id) {
        log.info("handle /picDetailAjax, id=" + id);
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        if (flow1000Section == null) {
            return new SectionDetail();
        }
        List<ImgDetail> imgDetailList = flow1000Section.getImages().stream()
                .map(image -> new ImgDetail(image.getName(), image.getWidth(), image.getHeight()))
                .collect(Collectors.toList());

        return new SectionDetail(flow1000Section.getDirName(), flow1000Section.getId(), imgDetailList,
                flow1000Section.getAlbum());
    }

    @RequestMapping("/picContentAjax")
    public SectionContent picContentAjax(@RequestParam(value = "id", defaultValue = "1") Long id) {
        log.info("handle /picDetailAjax, id=" + id);
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);

        List<String> imgList = flow1000Section.getImages().stream().map(image -> image.getName())
                .collect(Collectors.toList());

        return new SectionContent(flow1000Section.getDirName(), flow1000Section.getId().intValue(), imgList);
    }

    @RequestMapping("/searchSection")
    public List<Flow1000Section> searchSection(@RequestParam(value = "name", defaultValue = "") String name) {
        log.debug("searchSection request, name=" + name);
        if ("".equals(name)) {
            return new ArrayList<Flow1000Section>();
        }
        name = "%" + name + "%";

        return local1000SectionDao.searchFlow1000SectionByName(name);
    }

    @RequestMapping("/picIndexAjax")
    public List<PicIndex> picIndexAjax(
            @RequestParam(value = "time_stamp", defaultValue = "19700101000000") String timeStamp,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(value = "client_status", required = false) String clientStatus,
            @RequestParam(value = "album", required = false) String album) {
        log.info("handle /picIndexAjax, timeStamp=" + timeStamp);
        Flow1000Section.ClientStatus clientStatusCondition = null;
        if (clientStatus != null && !clientStatus.isEmpty()) {
            try {
                clientStatusCondition = Flow1000Section.ClientStatus.valueOf(clientStatus);
            } catch (IllegalArgumentException ignored) {
            }
        }

        Flow1000Section.ClientStatus finalClientStatusCondition = clientStatusCondition;
        List<Flow1000Section> flow1000SectionList = local1000SectionDao.findAll(
                (Specification<Flow1000Section>) (root, query, builder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (album != null && !album.isEmpty()) {
                        Predicate albumPredicate = builder.equal(root.get("album"), album);
                        predicates.add(albumPredicate);
                    }

                    if (searchKey != null && !searchKey.isEmpty()) {
                        String name = "%" + searchKey + "%";
                        Predicate albumPredicate = builder.like(root.get("name"), name);
                        predicates.add(albumPredicate);
                    }
                    if (finalClientStatusCondition != null)  {
                        Predicate albumPredicate = builder.equal(root.get("clientStatus"), finalClientStatusCondition);
                        predicates.add(albumPredicate);
                    }
                    Predicate createTimePredicate = builder.greaterThan(root.get("createTime"), timeStamp);
                    predicates.add(createTimePredicate);
                    return builder.and(predicates.toArray(new Predicate[] {}));
                });

        return flow1000SectionList.stream().map(flow1000Section -> PicIndex.builder()
                .index(flow1000Section.getId())
                .name(flow1000Section.getDirName())
                .mtime(flow1000Section.getCreateTime())
                .cover(flow1000Section.getCover())
                .coverHeight(flow1000Section.getCoverHeight())
                .coverWidth(flow1000Section.getCoverWidth())
                .album(flow1000Section.getAlbum())
                .clientStatus(PicIndex.ClientStatus.valueOf(flow1000Section.getClientStatus().name()))
                .build()).collect(Collectors.toList());
    }


    @RequestMapping(value = "/urls1000", method = { RequestMethod.POST })
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
                fileUtil.getFileNameByUrl(urls1000Body.getImgSrcArray().get(0).getSrc()));
        flow1000Section.setAlbum("flow1000");
        List<Flow1000Img> flow1000ImgList = new ArrayList<>();
        for (Urls1000Body.ImgSrcBean imgSrcBean : urls1000Body.getImgSrcArray()) {
            String fileName = fileUtil.getFileNameByUrl(imgSrcBean.getSrc());
            Flow1000Img flow1000Img = new Flow1000Img();
            flow1000Img.setName(fileName);
            flow1000Img.setInCover(
                    urls1000Body.getImgSrcArray().lastIndexOf(imgSrcBean) == 0 ? 1 : 0);
            flow1000Img.setSrc(imgSrcBean.getSrc());
            flow1000Img.setHref(imgSrcBean.getRef());
            flow1000ImgList.add(flow1000Img);
        }
        downloadSectionThreadPoolExecutor.execute(() -> {
            CountDownLatch countDownLatch = new CountDownLatch(flow1000ImgList.size());

            for (Flow1000Img flow1000Img : flow1000ImgList) {
                downloadImgThreadPoolExecutor.execute(new DownloadImgRunnable(
                        flow1000Img, dirName, countDownLatch, baseDir));
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException ignored) {
            }
            log.info(flow1000Section.getDirName() + " download complete");
            // local1000SectionDao.insertFlow1000Section(flow1000Section);
            flow1000ImgList.forEach(flow1000Img -> {
            });
            // local1000ImgDao.insertFlow1000Img(flow1000ImgList);

            PicIndex picIndex = PicIndex.builder()
                    .index(flow1000Section.getId().intValue())
                    .name(flow1000Section.getDirName())
                    .mtime(flow1000Section.getCreateTime())
                    .cover(flow1000Section.getCover())
                    .coverHeight(flow1000Section.getCoverHeight())
                    .coverWidth(flow1000Section.getCoverWidth())
                    .album(flow1000Section.getAlbum())
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            try {
                wsMsgService.sendWsMsg(mapper.writeValueAsString(picIndex));
            } catch (Exception ignored) {
            }
        });
    }

    @RequestMapping(value = "/deleteSection", method = { RequestMethod.POST })
    public void deleteSection(@RequestBody SectionDetail sectionDetail) {
        log.info("handle /deleteSection, sectionDetail=" + sectionDetail.toString());
        if (sectionDetail.getId() == null || sectionDetail.getId() <= 0) {
            return;
        }
        local1000ImgDao.deleteById(sectionDetail.getId());
        local1000SectionDao.deleteById(sectionDetail.getId());
    }

    private String[] listImages(String dirName) {
        File dirFile = new File(baseDir + "/source" + "/" + dirName);
        return dirFile.list((dir, fileName) -> fileName != null && (fileName.endsWith(".jpg")
                || fileName.endsWith(".JPG")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".JPEG")
                || fileName.endsWith(".png")
                || fileName.endsWith(".PNG")));
    }

    private static int compareImgName(String name1, String name2) {
        name1 = name1.split("\\.")[0];
        name2 = name2.split("\\.")[0];

        try {
            return Integer.parseInt(name1) - Integer.parseInt(name2);
        } catch (Exception e) {
            return name1.compareTo(name2);
        }
    }

    private Flow1000Img generate1000Img(String imgName, Flow1000Section flow1000Section) {
        try {
            BufferedImage sourceImg = ImageIO.read(Files.newInputStream(
                    Paths.get(baseDir + "/source" + "/" + flow1000Section.getDirName() + "/" + imgName)));
            int width = sourceImg.getWidth();
            int height = sourceImg.getHeight();
            Flow1000Img flow1000Img = new Flow1000Img();
            flow1000Img.setName(imgName);
            flow1000Img.setHeight(height);
            flow1000Img.setWidth(width);
            flow1000Img.setFlow1000Section(flow1000Section);
            return flow1000Img;
        } catch (IOException ignored) {
        }
        return null;
    }

    @PostMapping("/downloadSection")
    public ResponseEntity<PicIndex> downloadSection(@RequestParam(value = "id", defaultValue = "1") Long id) {
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        if (flow1000Section == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        flow1000Section.setClientStatus(Flow1000Section.ClientStatus.PENDING);
        local1000SectionDao.saveAndFlush(flow1000Section);
        PicIndex picIndex = PicIndex.builder()
                .index(flow1000Section.getId().intValue())
                .name(flow1000Section.getDirName())
                .mtime(flow1000Section.getCreateTime())
                .cover(flow1000Section.getCover())
                .coverHeight(flow1000Section.getCoverHeight())
                .coverWidth(flow1000Section.getCoverWidth())
                .album(flow1000Section.getAlbum())
                .build();
        return ResponseEntity.ok(picIndex);
    }

    @PostMapping("/completeSection")
    public ResponseEntity<PicIndex> completeSection(@RequestParam(value = "id", defaultValue = "1") Long id) {
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        if (flow1000Section == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        flow1000Section.setClientStatus(Flow1000Section.ClientStatus.LOCAL);
        local1000SectionDao.saveAndFlush(flow1000Section);
        PicIndex picIndex = PicIndex.builder()
                .index(flow1000Section.getId().intValue())
                .name(flow1000Section.getDirName())
                .mtime(flow1000Section.getCreateTime())
                .cover(flow1000Section.getCover())
                .coverHeight(flow1000Section.getCoverHeight())
                .coverWidth(flow1000Section.getCoverWidth())
                .album(flow1000Section.getAlbum())
                .build();
        return ResponseEntity.ok(picIndex);
    }

//    @PostMapping("/deleteSection")
    public ResponseEntity<PicIndex> deleteSection(@RequestParam(value = "id", defaultValue = "1") Long id) {
        Flow1000Section flow1000Section = local1000SectionDao.queryFlow1000SectionById(id);
        if (flow1000Section == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        flow1000Section.setClientStatus(Flow1000Section.ClientStatus.NONE);
        local1000SectionDao.saveAndFlush(flow1000Section);
        PicIndex picIndex = PicIndex.builder()
                .index(flow1000Section.getId().intValue())
                .name(flow1000Section.getDirName())
                .mtime(flow1000Section.getCreateTime())
                .cover(flow1000Section.getCover())
                .coverHeight(flow1000Section.getCoverHeight())
                .coverWidth(flow1000Section.getCoverWidth())
                .album(flow1000Section.getAlbum())
                .build();
        return ResponseEntity.ok(picIndex);
    }

    @GetMapping("/albumConfig/list")
    public List<AlbumConfig> albumConfigList() {
        return local1000AlbumConfigDao.findAll();
    }
}
