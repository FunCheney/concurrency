## ConcurrentHashMap
&ensp;&ensp;ConcurrentHashMap是线程安全的HashMap；在并发的情况下使用HashMap可能会导致死循环，在进行put操作时导致CPU利用率接近100%。是因为在多线程会导致HashMap的Entry链表形成环形数据结构，一旦形成环形数据结构，Entry的next结点永远不能为空，就会产生死循环获取Entry。

&ensp;&ensp;在JDk1.8中ConcurrentHashMap采用Node + CAS + Synchronized来保证并发情况下的更新不会出现问题。其底层的数据结构是：数组 + 链表 + 红黑树 的方式来实现的。

注：[点击了解红黑树]。

### ConcurrentHashMap中的成员

#### 关键常量
```
/** 
  * 最大容量，32位的Hash值的最高两位用作控制的目的，
  * 这个值必须恰好是1<<30(2的30次方)，这样分配的java数组
  * 在索引范围内(2的整数次幂)。
  */
private static final int MAXIMUM_CAPACITY = 1 << 30;

/**
 * Hash表默认的初始容量。必须是2的整数次幂，
 * 最小为1，最大为MAXIMUM_CAPACITY.
 */
private static final int DEFAULT_CAPACITY = 16;

/**
 * 最大的数组大小(非2次幂)。
 * toArray 和 related方法使用。
 */
static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

/**
 * 表默认的并发级别，未使用。为与该类的以前版本兼容而定义
 */
private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

/**
 * T表的默认加载因子，在构造函数中重写此值只影响初始表容量。
 * 浮点值通常不被使用，
 * 当前表中的容量 = 初始化容量 - (初始化容量无符号右移2位)时扩容
 */
private static final float LOAD_FACTOR = 0.75f;

/**
 * 链表转红黑树阀值,该值必须大于2，并且应该至少为8。
 * 以便与树移除中关于收缩后转换回普通Bin的假设相吻合。
 */
static final int TREEIFY_THRESHOLD = 8;

/**
 * 用于在调整大小操作期间反树化(拆分)bin的bin计数阈值，
 * 应该小于TREEIFY_THRESHOLD, 最多为6.
 */
static final int UNTREEIFY_THRESHOLD = 6;

static final int MIN_TREEIFY_CAPACITY = 64;

private static final int MIN_TRANSFER_STRIDE = 16;

/**
 * 用于生成戳记的位的数目，单位为sizeCtl。
 * 32位数组必须至少为6.
 */
private static int RESIZE_STAMP_BITS = 16;

/**
 * 2^15-1，help resize的最大线程数
 */
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

/**
 * 32-16=16，sizeCtl中记录size大小的偏移量
 */
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

/* forwarding nodes的hash值*/
static final int MOVED     = -1; 

/* 树根节点的hash值*/
static final int TREEBIN   = -2; 

/* ReservationNode的hash值*/
static final int RESERVED  = -3; 

/* 普通节点哈希的可用位*/
static final int HASH_BITS = 0x7fffffff;
```

#### 关键属性
```
/**
 * 装载Node的数组，作为ConcurrentHashMap的数据容器，
 * 采用懒加载的方式，直到第一次插入数据的时候才会进行初始化操作，
 * 数组的大小总是为2的幂次方。
 */
transient volatile Node<K,V>[] table;

/**
 * 扩容时使用，只有在扩容的时候才为非null
 */
private transient volatile Node<K,V>[] nextTable;


/**
 * 控制Table的初始化与扩容。
 *   当值为负数时table正在被初始化或扩容
 *     -1表示正在初始化
 *     -N则表示当前正有N-1个线程进行扩容操作
 *   正数或0代表hash表还没有被初始化，这个数值表示初始化或下一次进行扩容的大小
 */
private transient volatile int sizeCtl;
```

#### 内部类

##### Node 类
&ensp;&ensp; Node是最核心的内部类，它包装了key-value键值对，所有插入ConcurrentHashMap的数据都包装在这里面。Node类实现了Map.Entry<K,V>接口，Node类中包含有属性有key，value以及下一节点的引用，其中value和next属性使用volatile关键字修饰，保证其在多线程下的可见性。不允许调用setValue方法直接改变Node的value域，它增加了find方法辅助map.get()方法。

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;
    /**
    * Node结点的构造方法
    */
    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    public final K getKey()       { return key; }
    public final V getValue()     { return val; }
    public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
    public final String toString(){ return key + "=" + val; }
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
                (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                (v = e.getValue()) != null &&
                (k == key || k.equals(key)) &&
                (v == (u = val) || v.equals(u)));
    }
    
    /**
     * Node结点中提供的find方法，在子类中可重写 
     */
    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        if (k != null) {
            do {
                K ek;
                if (e.hash == h &&
                    ((ek = e.key) == k || (ek != null && k.equals(ek))))
                    return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}
```

##### TreeNode类
&ensp;&ensp;树节点类，另外一个核心的数据结构，包含父接点，左链接的结点，右链接的结点，前驱结点的引用，以及结点的颜色(默认红色)。当链表长度过长的时候，会转换为TreeNode在TreeBins中使用。TreeNode是上述Node类的子类。
```java

static final class TreeNode<K,V> extends Node<K,V> {
    TreeNode<K,V> parent;  // red-black tree links
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    // needed to unlink next upon deletion
    boolean red;

    
    TreeNode(int hash, K key, V val, Node<K,V> next,
             TreeNode<K,V> parent) {
        super(hash, key, val, next);
        this.parent = parent;
    }

    Node<K,V> find(int h, Object k) {
        return findTreeNode(h, k, null);
    }

    /**
     * 通过给定的key从指定的根节点开始(在其子树)查找
     * 对应的TreeNode结点，没有返回null
     * h 表示当前可以的Hash值
     * k 要查找的键(key)
     * kc k的Class对象，该Class应该是实现了Comparable<K>的，否则应该是null
     */
    final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
        // 判断对应的键是否为null
        if (k != null) {
            //获取当前结点
            TreeNode<K,V> p = this;
            do  { //循环
                int ph, dir; K pk; TreeNode<K,V> q;
                TreeNode<K,V> pl = p.left, pr = p.right;
                if ((ph = p.hash) > h)
                    /**
                    *  当前结点的Hash值大于要查找的Key的Hash值H
                    *  在当前节点的左子树中查找，反之在右子树中
                    *  进行下一轮循环
                    */
                    p = pl;
                else if (ph < h)
                    p = pr;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    /**
                    *  当前结点的key等于要查找的key，
                    *  或当前结点的key不为null且equals()方法为true
                    *  返回当前的结点
                    */
                    return p;
                
                    
                /**
                * 执行到这里说明 hash比对相同，
                * 但当前节点的key与要查找的k不相等
                */ 
                else if (pl == null)
                    /**
                    * 左孩子为空，指向当前节点右孩子，继续循环
                    */
                    p = pr;
                else if (pr == null)
                    /**
                     * 右孩子为空，指向当前节点左孩子，继续循环
                     */
                    p = pl;
                /**
                 * 左右孩子都不为空，再次进行比较，
                 * 确定在左子树还是右子树中查找 
                 */    
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)
                    /**
                     * comparable方法来比较pk和k的大小
                     * dir小于0，p指向左孩子，否则指向右孩子
                     */
                    p = (dir < 0) ? pl : pr;
                    
                /**
                 *  无法通过上一步骤确定是在左/右子树中查找
                 *  从右子树中递归调用findTreeNode()方法查找
                 */    
                else if ((q = pr.findTreeNode(h, k, kc)) != null)
                    return q;
                else
                    //在右子树中没有找到，到左子树中查找
                    p = pl;
            } while (p != null);
        }
        return null;
    }
}
```

##### TreeBin类
&ensp;&ensp;红黑树结构。该类并不包装key-value键值对，而是TreeNode的列表和它们的根节点。它代替了TreeNode的根节点，也就是说在实际的ConcurrentHashMap“数组”中，存放的是TreeBin对象，而不是TreeNode对象。这个类含有读写锁。
这里我们先看红黑树相关操作的方法。

```java
static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;
        volatile TreeNode<K,V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        /**
         * 通过结点b构造红黑树，链表转红黑树
         */
        TreeBin(TreeNode<K,V> b) {
            super(TREEBIN, null, null, null);
            // 将给定的节点指向头结点
            this.first = b;
            TreeNode<K,V> r = null;
            /**
             * 定义X节点 为 b 结点；next结点也为b结点
             * next 节点初始化为头结点，用来控制遍历
             */
            for (TreeNode<K,V> x = b, next; x != null; x = next) {
                //指向下一结点
                next = (TreeNode<K,V>)x.next;
                //将x结点的连接属性清空
                x.left = x.right = null;
                if (r == null) {
                    /**
                     *  r 为null 说明是红黑树中没有结点
                     *  x 结点就是红黑树的根结点
                     *    根结点的父结点为null，颜色为黑色
                     */
                    x.parent = null;
                    x.red = false;
                    r = x;
                }
                else {
                    // 当前结点的关键字
                    K k = x.key;
                    // 当前结点的hash值
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = r;;) {
                        /**
                         * 从红黑树的根结点开始遍历，查找当前结点对应的位置
                         * dir 控制查找的方向
                         * ph 记录当前结点的hash值
                         */
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            /**
                             * 当前节点的hash值大于要插入结点的hash值，
                             * 在当前结点的左子树中查找
                             */
                            dir = -1;
                        else if (ph < h)
                            /**
                             * 当前节点的hash值小于要插入结点的hash值，
                             * 在当前结点的右子树中查找
                             */
                            dir = 1;
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            /**
                             * 如果hash值相等，则比较k值，用其Compare，
                             * 如果还相等，则走tieBreakOrder方法
                             */
                            dir = tieBreakOrder(k, pk);
                            // 暂存当前节点
                            TreeNode<K,V> xp = p;
                            
                        /**
                         * 根据dir控制查找方向
                         */    
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            
                            // 插入后平衡红黑树的性质
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            //指定红黑树的根结点
            this.root = r;
            assert checkInvariants(root);
        }
        
        /**
         * 左旋转过程
         */
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            
            TreeNode<K,V> r, pp, rl;
            /**
             * 结点P不为null且p的右结点不为null 
             */
            if (p != null && (r = p.right) != null) {
                
                if ((rl = p.right = r.left) != null)
                    /**
                     * p.right = r.left p的右结点为r的左结点
                     * 然后将其赋值给rl
                     * 当rl 不为空的时候，确定p与rl的关系：
                     *   父结点与左子结点
                     */
                    rl.parent = p;
                
                if ((pp = r.parent = p.parent) == null)
                    /**
                     * 结合 r = p.right
                     *    r.parent = p.parent 将p的右子树链接到 p的父结点
                     * 如果P的父结点为null，说明当前p结点为红黑树的根结点
                     *    经过上述r.parent = p.parent 将红黑树的根节点转为r
                     *      根结点为r结点，颜色尾黑色
                     */
                    (root = r).red = false;
                else if (pp.left == p)
                    /**
                     * 到这一步说明 p 结点不是红黑树的根结点
                     * 且p为其父结点的左子树，
                     * 将r替换原来P结点的位置(左旋转)
                     */
                    pp.left = r;
                else
                    /**
                     * 到这一步说明 p 结点不是红黑树的根结点
                     * 且p为其父结点的右子树，
                     * 将r替换原来P结点的位置(左旋转)
                     */
                    pp.right = r;
                    
                /**
                 * 上述过程只是完成了将原先以P 结点为红黑树子树
                 * 的根结点，替换为以P的右结点为根结点的部分
                 * 即 p.right = r.left 将原先r的左链接替换
                 * 成 p的右链接的过程。
                 */    
                
                //r的左结点为p
                r.left = p;
                // p的父结点为r节点
                p.parent = r;
            }
            return root;
        }

        /**
         *  右旋转 为上述左旋转的逆过程
         */
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }
        
        /**
         * 红黑树中插入结点后会打破红黑树性质需要平衡 
         * TreeNode<K,V> root 根结点
         * TreeNode<K,V> x 要插入的结点
         */
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            //默认插入结点为红色
            x.red = true;
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                // xp为当前节点的父结点
                if ((xp = x.parent) == null) {
                    /**
                     * 当前结点的父结点为空，说明红黑树中只有一个结点
                     * 当前结点即为根结点，颜色为黑色
                     */
                    x.red = false;
                    return x;
                }
                /**
                 * 当前结点的父结点（xp）不为null
                 *   父结点为黑色，没有打破红黑树的平衡性(着色可能有问题)
                 *   父结点的的父结点(xpp)为null，红黑树中只有两个节点
                 *   上述两种情况直接返回root结点
                 */
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                
                /**
                * 当前结点的父结点(xp) 为 其父节点(xpp)的左孩子 
                */
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        /**
                         *  当前结点(x)得父结点(xp)的父结点(xpp)的右孩子(xppr)
                         *  不为null 且 颜色为红色(此时颜色的性质不满足)
                         *  变换颜色
                         */
                        xppr.red = false; // 将xppr变为黑色
                        xp.red = false;   // 将xp变为黑色  
                        xpp.red = true;   // 将xpp变为红色 
                        x = xpp; // 将xpp指向x 继续循环
                    }
                    /**
                     * 当前结点的父结点的父结点右孩子为null或颜色为黑色
                     */
                    else {
                        // 如果(当前结点)x为父结点的右孩子
                        if (x == xp.right) {
                            //左旋转
                            root = rotateLeft(root, x = xp);
                            // 重新指定xpp
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        // 如果当前结点的父结点不为null
                        if (xp != null) {
                            // 将xp的颜色置为黑色
                            xp.red = false;
                            // 父结点的父结点(xpp)不为null
                            if (xpp != null) {
                                //将xpp颜色置为红色
                                xpp.red = true;
                                // 有旋转
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }
                
                /**
                 * 当前结点的父结点(xp) 为 其父节点(xpp)的右孩子 
                 */
                else {
                    //xppl 为当前结点(x)的父结点(xp)的父结点(xpp)的左孩子
                    if (xppl != null && xppl.red) {
                        /**
                         *  xppl不为null 且是红结点
                         *          xpp                   
                         *         /  \
                         *  red  xppl  xp red
                         *                   ---> x结点在这一层
                         *           | 变为
                         *          xpp red  ---> 变换过后x结位置
                         *          /  \
                         * black xppl  xp black 
                         *              
                         */
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        // 控制循环
                        x = xpp;
                    }
                    else {
                        //如果左叔叔为空或者是黑色
                        if (x == xp.left) {
                            //如果当前节点是个左孩子 右旋转                  
                            root = rotateRight(root, x = xp);
                            //获取爷爷结点
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            /**
                             * 父结点不为null 设置父结点为黑色 
                             */
                            xp.red = false;
                            if (xpp != null) {
                                /**
                                 * 爷爷结点不为null
                                 * 将其置为红色
                                 * 对其进行左旋转
                                 */
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }
        
        /**
         * 红黑树中删除节点后会打破红黑树的性质需要平衡
         * TreeNode<K,V> root 根结点
         * TreeNode<K,V> x 要删除的节点
         */
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                if (x == null || x == root)
                    // x 为 null 或者 x 为根结点 无须平衡
                    return root;
                else if ((xp = x.parent) == null) {
                    /**
                     *      xp null
                     *       \
                     *        ---> x 结点位置(可为左结点也可为右结点) 
                     * 此时x 为红黑树的根结点
                     */
                    x.red = false;
                    return x;
                }
                else if (x.red) {
                     /**
                      *      xp 
                      *       \
                      *        ---> x 结点位置(且为red)
                      *  将其变为黑色结点
                      */
                    x.red = false;
                    return root;
                }
                
                else if ((xpl = xp.left) == x) {
                    // x 为其父结点的左结点
                    if ((xpr = xp.right) != null && xpr.red) {
                        /**
                         *      xp 
                         *     / \
                         *    x  xpr red
                         *       / \
                         *   xprl   xprr
                         */
                        xpr.red = false; // 将xpr置为黑色
                        xp.red = true;   // 将xp置为红色
                        // 左旋转 xp
                        root = rotateLeft(root, xp);
                        /**
                         *      xpr 
                         *     / \
                         *    xp  xprr
                         *   / \
                         *  x   xprl
                         */
                        // 获取 新的xpr
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    
                    if (xpr == null)
                        // 控制循环
                        x = xp;
                    else {
                        /**
                         *      xpr 
                         *     / \
                         *    xp  xprr
                         *   / \
                         *  x   xprl
                         *       / \
                         *     sl  sr
                         */
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            /**
                             * 树中xpr(即上图的xprl) 的叶子结点不存在 或为黑色结点
                             * 树中xpr(上图中的xprl) 置为红色
                             * 将x 指向 xp 继续循环
                             */
                            xpr.red = true;
                            x = xp;
                        }
                        else {
                            if (sr == null || !sr.red) {
                                /**
                                 *      xpr 
                                 *     / \
                                 *    xp  xprr
                                 *   / \
                                 *  x   xprl
                                 *       / \
                                 *         sr null || black
                                 */
                                if (sl != null)
                                    // sl 不为null 将其置为黑色
                                    sl.red = false;
                                // 树中的xpr(上图xprl)置为红色
                                xpr.red = true;
                                // 右旋转
                                root = rotateRight(root, xpr);
                                // 重新获取xp的右子树
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }
                            if (xpr != null) {
                                /**
                                 * 重新获取的xpr 不为null
                                 * xpr 的颜色与 父结点的颜色相同
                                 */
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                // 重新获取的xpr 的右节点不为null，将其置黑
                                sr.red = false; 
                            }
                            if (xp != null) {
                                /**
                                 * xp 不为null 
                                 * 将xp置为红色
                                 * 左旋转xp
                                 */
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                    }
                }
            }
            else { // x 为其父结点的右结点
                if (xpl != null && xpl.red) {
                    /**
                     *      xp 
                     *     / \
                     *  xpl   x
                     *       / \
                     *      
                     */
                    xpl.red = false;
                    xp.red = true;
                    /**
                     *        xp            xpl
                     *       / \            / \
                     *     xpl   x  ==>  xpll  xp   
                     *    / \   / \           /  \
                     * xpll xplr           xplr  x
                     */
                    root = rotateRight(root, xp);
                    /**
                     * 重新获取xpl
                     *          xpl
                     *          / \
                     *       xpll  xp   
                     *            /  \
                     *          xplr  x
                     *           |__ 新的xpl指向这里
                     */
                    xpl = (xp = x.parent) == null ? null : xp.left;
                }
                if (xpl == null)
                    // 新的xpl为null x 指向器父结点
                    x = xp;
                else {
                    // 获取xpl的左结点与右结点
                    TreeNode<K,V> sl = xpl.left, sr = xpl.right;
                    if ((sl == null || !sl.red) &&
                        (sr == null || !sr.red)) {
                        /**
                         * 左子结点 为空或 为黑色
                         * 且
                         * 右子结点 为空或 为黑色
                         * 
                         * 将 xpl 置为红色
                         */
                        xpl.red = true;
                        // 控制循环
                        x = xp;
                    }
                    else {
                        if (sl == null || !sl.red) {
                            /**
                             *        xp 
                             *       / \
                             *    xpl   x
                             *    / \  / \
                             *  sl(null || black)
                             */
                            if (sr != null)
                                // 如果sr不为null 设置为黑色
                                sr.red = false;
                            //xpl置为红色
                            xpl.red = true;
                            //左旋转xpl
                            root = rotateLeft(root, xpl);
                            //重新获取xpl
                            xpl = (xp = x.parent) == null ?
                                null : xp.left;
                        }
                        if (xpl != null) {
                            // xpl 不为null xpl的颜色与xp的颜色相同
                            xpl.red = (xp == null) ? false : xp.red;
                            if ((sl = xpl.left) != null)
                                sl.red = false;
                        }
                        if (xp != null) {
                            /**
                             * xp不为null
                             * xp为黑色
                             * 右旋转xp
                             */
                            xp.red = false;
                            root = rotateRight(root, xp);
                        }
                        x = root;
                    }
                }
            }
        }
    }
}
```

**总结:**

&ensp;&ensp;在上一篇文章中，系统的学习了红黑树相关的知识，包括性质，以及为了维护红黑树的性质需要进行相应的左旋转，右旋转，颜色转换等子过程。在学习ConcurrentHashMap之前，要先对红黑树的操作有一定的了解。这篇文章，从ConcurrentHashMap的底层，通过其内部定义的一些常量，以及相关的内部类看起，重新回顾了一下红黑树的操作，以及并发大师的实现方式。在下一篇文章中，将学习ConcurrentHashMap相关的操作以及实现原理。


 [点击了解红黑树]:https://mp.weixin.qq.com/s/FzNbESz6FWdCayVyRD3YFA