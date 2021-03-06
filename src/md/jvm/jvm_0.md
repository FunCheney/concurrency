### 初始JVM
&ensp;&ensp;JVM是Java Virtual Machine 的简称。意为Java虚拟机。

### Java中字节码运行的方式
#### 解释运行
&ensp;&ensp;解释执行以解释方式运行字节码，解释执行就是：读一句执行一句。
#### 编译运行(JIT)
&ensp;&ensp;编译运行就是将字节码编译成机器码，直接执行机器码。在程序运行的时候编译，编译后性能有数量级的提升。

#### Java程序的执行方式：

 * 静态编译执行
 * 动态编译执行
 * 动态解释执行
 
 &ensp;&ensp;Java语言采用编译 + 解释来执行的。
 
 
**为什么使用JVM：**


### JVM启动流程
![image](https://github.com/FunCheney/concurrency/blob/master/src/md/jvm/image/JVM%E5%90%AF%E5%8A%A8%E6%B5%81%E7%A8%8B.jpg "JVM启动流程")

&ensp;&ensp;JVM的启动是由Java命令来启动的，java命令会跟上一个启动类，启动类中会有main()方法。

* step1: 装载配置，根据当前路径和系统版本寻找JVM的配置文件；

* step2: 找到配置文件之后，会找JVM.dll文件，这个文件是JVM主要实现

* step3: 找到匹配当前系统版本的dll文件之后，会初始化先关的虚拟机。

* step4: 找到main方法，启动类。

### JVM基本结构
![image](https://github.com/FunCheney/concurrency/blob/master/src/md/jvm/image/JVM%E5%9F%BA%E6%9C%AC%E7%BB%93%E6%9E%84.jpg "JVM基本结构")

* PC寄存器
     
      每个线程拥有一个PC寄存器
      在线程创建时创建
      指向下一条指令的地址
      执行本地方法时，PC的值为undefined

* 方法区    
 
  保存内的装载信息
  
        类的常量池
        字段、方法信息
        方法字节码
  
* java堆

&ensp;&ensp;Java对是全局共享的，所有线程都可以访问

* java栈
  
      线程私有
      栈有一些列的栈帧组成（java栈也叫做帧栈）
      帧保存一个方法的局部变量、操作数，常量池指针
      每一次方法调用创建一个帧，并压栈
             
   **java栈--局部变量表 包含参数和局部变量**
   
   **java栈--函数调用组成栈帧**    
   
   **java栈--操作数栈**
       
        java没有寄存器，所有参数传递使用操作数栈
        
   **java栈--栈上分配**
        
        栈上分配(方法的局部变量)，不会出现内存泄露。
        小对象（一般几十个bytes），在没有逃逸（没有其他线程使用）的情况下，可以直接分配在栈上
        直接分配在栈上，可以自动回收，减轻GC压力
        大对象或者逃逸对象无法在栈上分配
        
 **问题：**
 
 为了能让递归函数调用次数更多一些，应该怎么做呢？  


     
### JIT编译优化

#### 字表达式消除
#### 方法内联
#### 逃逸分析
* 全局变量赋值逃逸
* 方法返回值逃逸
* 实例引用发生逃逸
* 线程逃逸
#### 对象的栈上内存分配
#### 标量替换
#### 同步锁的消除