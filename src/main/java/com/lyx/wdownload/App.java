package com.lyx.wdownload;

import com.lyx.OptRuntimeException;
import com.lyx.thrid.hutool.core.collection.CollUtil;
import com.lyx.thrid.hutool.core.exceptions.ExceptionUtil;
import com.lyx.thrid.hutool.core.io.FileUtil;
import com.lyx.thrid.hutool.core.io.IoUtil;
import com.lyx.thrid.hutool.core.lang.Console;
import com.lyx.thrid.hutool.core.text.CharSequenceUtil;
import com.lyx.thrid.hutool.core.util.RandomUtil;
import com.lyx.thrid.hutool.core.util.StrUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class App
{
    /**
     * 配置项 <br/>
     * 图片下载到这个目录
     */
    private final static String DEST_DIR = "/Users/leeyx/my_dir/download/wallhaven_batch_download";

    /**
     * 配置项 <br/>
     * 要下载的、筛选出来的图片，没有页码参数 <br/>
     * 是从控制台中获取的，而不是浏览器地址栏里的地址. <br/>
     * 不能进行url编码 <br/>
     * 获取这个链接教程：<a href="https://www.dropbox.com/s/4gh9qgyfpvwc0u5/%E5%B1%8F%E5%B9%95%E5%BD%95%E5%88%B62023-04-25%2015.32.27.mov?dl=0">点我查看</a>
     */
    private final static String DOWNLOAD_URL = "https://wallhaven.cc/search?q=id:124082&categories=111&purity=110&sorting=relevance&order=desc&ai_art_filter=0";

    /**
     * 配置项 <br/>
     * 总页码，可以从wallhaven页面上获取
     */
    private final static Integer pageNum = 7;

    /**
     * 工具-RestTemplate
     */
    private final static RestTemplate REST_TEMPLATE = new RestTemplate();

    public static void main(String[] args)
    {
        // 一页一页地下载
        for (int i=1; i<=pageNum; i++)
        {
            List<String> pageHref = getPageData(i);
            pageHref.forEach(App::downloadOnePicByPageHref);
        }
        Console.log("下载完成");
    }

    /**
     * 获取分页页面上的链接
     * @param pageNum 页码
     * @return 分页页面上的链接，点一下会进入到壁纸详情
     */
    private static List<String> getPageData(int pageNum)
    {
        Document doc;
        try
        {
            doc = Jsoup.connect(StrUtil.format("{}&page={}", DOWNLOAD_URL, pageNum)).get();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw OptRuntimeException.getInstance("发生异常：{}", e.getMessage());
        }

        Elements elements = doc.select("section.thumb-listing-page ul li figure a.preview");
        Console.log("获取第 {} 页href成功", pageNum);
        return elements.stream().map(el -> el.attr("href")).collect(Collectors.toList());
    }

    /**
     * 通过分页页面上的一个url下载一张壁纸
     * @param pageHref
     */
    private static void downloadOnePicByPageHref(String pageHref)
    {
        Document doc;
        try
        {
            doc = Jsoup.connect(pageHref).get();
        }
        catch (IOException e)
        {
            throw OptRuntimeException.getInstance("发生异常：{}", e.getMessage());
        }

        String picUrl = doc.getElementsByTag("main").select("section div img").attr("src");
        downloadOnePic(picUrl);

        try
        {
            Thread.sleep(RandomUtil.randomLong(2600, 3500));
        }
        catch (InterruptedException e)
        {
            throw OptRuntimeException.getInstance(e.getMessage());
        }
    }

    /**
     * 下载一个壁纸
     * @param url 壁纸url
     */
    private static void downloadOnePic(final String url)
    {
        // 创建目录
        if (!FileUtil.exist(DEST_DIR))
        {
            FileUtil.mkdir(DEST_DIR);
        }

        // 根据url获取文件名
        int beginIndex = url.lastIndexOf(StrUtil.C_SLASH) + 1;
        String fileName = url.substring(beginIndex);

        // 下载文件到硬盘
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.USER_AGENT, "Application");
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = REST_TEMPLATE.exchange(url, HttpMethod.GET, httpEntity, byte[].class);
        FileUtil.writeBytes(response.getBody(), StrUtil.format("{}/{}", DEST_DIR, fileName));
        Console.log("下载 {} 成功", url);
    }
}