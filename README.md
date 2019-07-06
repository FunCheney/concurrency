# concurrency
并发编程学习
## 1. 线程的生命周期

线程间状态转换：

![image](https://github.com/FunCheney/concurrency/blob/master/src/Image/2.png "Thread Status")
* 1.new 新建状态，线程被创建但未启动。创建线程的方式有以下几种：
    1. 继承Thread类
    2. 实现Runnable接口
    3. 实现Callable接口
    
       a. 可以通过call()获得返回值。
       
       b. call() 可以捕获异常。
* 2.ready-to-run 就绪状态 竞争cpu资源。线程调用start()方法之后的，运行之前的状态
* 3.Running 运行状态，run()方法正在执行的状态
* 4.Dead 死亡状态
* 5.Sleeping 睡眠状态 超时等待 当一定时间过了之后，重新竞争cpu资源
* 6.Waiting 等待状态，执行了wait()方法 需要唤醒 才可以竞争cpu资源
* 7.Blocked 阻塞状态

## 2. 线程带来的风险
* 1.线程安全

* 2.活跃性问题
    1. 死锁
    2. 饥饿
    
        a. 高优先级吞噬低优先级的CPU时间片
        
        b. 线程被永久堵塞在一个等待进入同步块的状态
        
        c. 等待的线程永远不被唤醒
    3. 活锁
* 3.性能问题

## 3. 线程安全
&ensp;&ensp;编写线程安全的代码，本质上就是管理对**状态**(state)的访问,而且通常是**共享的、可变的状态**。
* 1 一个对象的**状态**就是他的数据，存储在**状态变量**(state variables)中，比如实例域或静态域。对象的状态还包括了其他附属对象的域。
* 2 **共享**，是指一个变量可以被多个线程访问。
* 3 **可变**，是指变量的值在其生命周期内可以改变。

&ensp;&ensp;当多个线程访问一个类时，如果不用考虑这些线程在运行时环境下的调度和交替执行，并且不需要额外的同步及在调用方代码
不必做其他的协调，这个类的行为任然是正确的，那么称这个类是**线程安全的**。

## 4. Java中操作共享数据安全程度
(由强至若排序)

* 1.不可变
* 2.绝对线程安全
* 3.相对线程安全
* 4.线程兼容
* 5.线程对立





 
 
