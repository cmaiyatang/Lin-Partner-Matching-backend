package com.younglin.partnerMatching;

import com.younglin.partnerMatching.model.domain.User;
import com.younglin.partnerMatching.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUserTest {
    @Resource
    private UserService userService;

    //线程设置
    private ExecutorService executorService = new ThreadPoolExecutor(16, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 循环插入用户
     * 批量插入用户
     */
    @Test
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();//计算插入时间
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("宇多田光");
            user.setUserAccount("false data");
            user.setUserPassword("12345678");
            user.setAvatarUrl("https://im.marieclaire.com.tw/m800c533h100b0/assets/mc/202210/6340372A3CFFF1665152810.jpeg");
            user.setProfile("熊光我是熊");
            user.setGender(0);
            user.setPhone("12345678900");
            user.setEmail("gibsonsybil48@gmail.com");
            user.setStatus(0);
            user.setUserRole(0);
            user.setTags("[]");
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }

    /**
     * 并发批量插入用户
     */
    @Test
    public void doConcurrencyInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        //分十组
        int j = 0;
        //批量插入数据的大小
        int batchSize = 2500;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        // i 要根据数据量和插入量来计算需要循环的次数，
        for (int i = 0; i < INSERT_NUM / batchSize; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
                User user = new User();
                user.setUsername("宇多田光");
                user.setUserAccount("false data");
                user.setUserPassword("12345678");
                user.setAvatarUrl("https://im.marieclaire.com.tw/m800c533h100b0/assets/mc/202210/6340372A3CFFF1665152810.jpeg");
                user.setProfile("熊光我是熊");
                user.setGender(0);
                user.setPhone("12345678900");
                user.setEmail("gibsonsybil48@gmail.com");
                user.setStatus(0);
                user.setUserRole(0);
                user.setTags("[]");
                userList.add(user);
                if (j % batchSize == 0) {
                    break;
                }
            }
            //异步执行 CompletableFuture<Void> future 创建每一个线程
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("ThreadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            }, executorService);
            //将线程加入到线程池中
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }


}
