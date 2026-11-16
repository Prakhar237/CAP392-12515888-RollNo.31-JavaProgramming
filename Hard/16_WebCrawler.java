import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Solution {
    public List<String> crawl(String startUrl, HtmlParser htmlParser) {
        String host=URI.create(startUrl).getHost();
        List<String> seen=new ArrayList<>();
        ExecutorService pool=Executors.newFixedThreadPool(8);
        AtomicInteger count=new AtomicInteger(1);
        CountDownLatch done=new CountDownLatch(1);

        seen.add(startUrl);
        search(startUrl,host,htmlParser,seen,pool,count,done);
        try{
            done.await();
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }finally{
            pool.shutdown();
        }
        return seen;
    }

    void search(String url, String host, HtmlParser parser, List<String> seen,
                ExecutorService pool, AtomicInteger count, CountDownLatch done) {
        pool.execute(()->{
            try{
                for(String link:parser.getUrls(url)){
                    if(host.equals(URI.create(link).getHost())){
                        boolean add=false;
                        synchronized(seen){
                            if(!seen.contains(link)){
                                seen.add(link);
                                add=true;
                            }
                        }
                        if(add){
                            count.incrementAndGet();
                            search(link,host,parser,seen,pool,count,done);
                        }
                    }
                }
            }finally{
                if(count.decrementAndGet()==0) done.countDown();
            }
        });
    }
}
