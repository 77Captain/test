package cn.hy.demo;


import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
class Main {

    public static void main(String[] args ) throws InterruptedException {
        Scanner input = new Scanner(System.in);
        System.out.println("请输入输出的长度");
        int length = input.nextInt();
        AwaitSignal awaitSignal = new AwaitSignal();
        //创建第一个休息室
        Condition first = null;
        if(length != 0){
            first = awaitSignal.newCondition();
        }
        //记录上一个休息室, 后面当作下一个休息室使用
        Condition prev = first;
        //记录所有线程
        List<Thread> threadList = new ArrayList<>();
        //记录第一个Runnable实例
        Task firstRunnable = null;

        //开启length个线程
        for (int i = 0; i < length; i++) {

            Thread thread;
            if(i == 0){
                //创建当前的休息室
                firstRunnable = new Task(first , awaitSignal);
                thread = new Thread(firstRunnable);
            }else{
                Condition curr = awaitSignal.newCondition();
                //创建当前的休息室
                thread = new Thread(new Task(curr ,prev , awaitSignal));
                prev = curr;
            }
            thread.setName("线程"+(i+1));
            threadList.add(thread);

        }

        //补充第一个线程的下一个休息室为最后一个休息室
        assert firstRunnable != null;
        firstRunnable.setNext(prev);

        //开启线程
/*        Collections.reverse(threadList);*/
        threadList.forEach(
                Thread::start
        );


        //由于线程一启动都进入了休息室等待，因此需要一个线程先唤醒a休息室中的线程
        TimeUnit.SECONDS.sleep(1);
        awaitSignal.lock();//获取锁
        try{
            log.info("由主线程唤醒第一个休息室，线程任务开始执行-----");
            first.signal();//唤醒a休息室的线程
        } finally {
            awaitSignal.unlock();//释放锁
        }

    }
}


class Task implements Runnable{

    private final AwaitSignal awaitSignal;

    //当前休息室
    private final Condition current;
    //下一个休息室
    private Condition next;

    public Task(Condition current ,Condition next ,AwaitSignal awaitSignal){
        this.current = current;
        this.next = next;
        this.awaitSignal = awaitSignal;
    }

    public Task( Condition current ,AwaitSignal awaitSignal){
        this.awaitSignal = awaitSignal;
        this.current = current;
    }

    public void setNext(Condition next) {
        this.next = next;
    }

    @Override
    public void run() {
        //输出a-z的字母
        for(int i = 97 ; i <= 122 ; i++){
            awaitSignal.print(String.valueOf((char)i) , current ,next);
        }
        //输出1-9的数字
        for(int j = 49 ; j <= 57 ; j++){
            awaitSignal.print(String.valueOf((char)j) , current ,next);
        }
    }
}

class AwaitSignal extends ReentrantLock {

    //每6个换行
    private static int number = 1;


    //写一个print，由多个线程调用，当不满足条件时进入格各自的休息室等待
    //参数1：打印的内容，参数2，进入哪一间休息室,参数3，表示下一件休息室
    public void print(String str, Condition current, Condition next){
        lock();//继承了ReentrantLock，可以省略前面的this
        try{
            current.await();//获得锁之后，先进入休息室等待被唤醒时表示可以继续运行执行它的打印了
            if(number % 6 == 0){
                System.out.println(str);
            }else{
                //每6个换行
                System.out.print(str);
            }
            number ++;
            next.signal();//打印完成之后去唤醒下一件休息室的线程
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlock();
        }
    }
}