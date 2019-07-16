### 线程间通信

&ensp;&ensp;线程拥有自己的栈空间，如若各个线程之前不能通信，其实对于多线程来说是没有意义的，因为只有各个线程之间相互协作，才能发挥多和处理器的最大价值。在前面Java内存模型的学习，也知道，java线程会将主内存的数据拷贝一份到自己的本地内存中，而线程之间通信的原理就是通过主内存来完成数据的共享。

#### 线程通行的方式
1).volatile

&ensp;&ensp;对于volatile关键字来说，我们前面已经做了详细的介绍，这里就不在赘述。对于其可以实现线程间的通信的原理，请查看volatile相关文章。 

2).synchronized

&ensp;&ensp;synchronized实现线程间通通信的原理，前面关于synchronized关键字的文章也做了较详细的介绍。我们以下图的方式在做一个总结：


3).等待/通知机制

&ensp;&ensp;首先，等待/通知机制是通过对象的wait(),notify()/notifyAll方法来实现的。

&ensp;&ensp;其次，等待通知机制需要与synchronized关键字配合使用。并且synchronized后面括号的部分必须是同一对象。

&ensp;&ensp;最后，等待通知机制的实现方式可以归纳出如下金典范式，该范式分为两部分，分别针对等待方(消费者)和通知方(生产者)

* 等待方遵循原则如下：


*通知方遵循原则如下：

