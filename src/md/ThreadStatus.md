### 线程安全
&ensp;&ensp;编写线程安全的代码，本质上就是管理对**状态**(state)的访问,而且通常是**共享的、可变的状态**。
* 1 一个对象的**状态**就是他的数据，存储在**状态变量**(state variables)中，比如实例域或静态域。对象的状态还包括了其他附属对象的域。
* 2 **共享**，是指一个变量可以被多个线程访问。
* 3 **可变**，是指变量的值在其生命周期内可以改变。

&ensp;&ensp;关于线程安全我们真正要做的，是在不可控制的并发访问中保护数据。一个对象是否应该是线程安全的取决于它是否会被多个线程访问。
线程安全的这个性质，取决于程序中如何使用对象。保证对象的线程安全性需要使用同步来协调其可变状态的访问；若做不到这一点，就会导致脏数据
和其他不可预期的后果。

&ensp;&ensp;当多个线程访问一个类时，如果不用考虑这些线程在运行时环境下的调度和交替执行，并且不需要额外的同步及在调用方代码
不必做其他的协调，这个类的行为仍然是正确的，那么称这个类是**线程安全的**。




 
 