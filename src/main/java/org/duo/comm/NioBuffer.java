package org.duo.comm;

import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Buffer类中有三个重要的成员属性：capacity（容量）、position（读写位置）和limit（读写的限制）
 * 1.capacity 属性
 * Buffer类的capacity属性表示内部容量的大小。一旦写入的对象数量超过了capacity，缓冲区就满了，不能再写入了。
 * 2.position属性
 * position属性的值与缓冲区的读写模式有关。在不同的模式下，position属性值的含义是不同的，在缓冲区进行读写的模式改变时，position值会进行相应的调整。
 * 在写模式下，position值的变化规则如下：
 * （1）在刚进入写模式时，position值为0，表示当前的写入位置为从头开始。
 * （2）每当一个数据写到缓冲区之后，position会向后移动到下一个可写的位置。
 * （3）初始的position值为0，最大可写值为limit-1。当position值达到limit时，缓冲区就已经无空间可写了。
 * 在读模式下，position值的变化规则如下：
 * （1）当缓冲区刚开始进入读模式时，position会被重置为0。
 * （2）当从缓冲区读取时，也是从position位置开始读。读取数据后，position向前移动到下一个可读的位置。
 * （3）在读模式下，limit表示可读数据的上限。position的最大值为最大可读上限limit，当position达到limit时表明缓冲区已经无数据可读。
 * 当新建了一个缓冲区实例时，缓冲区处于写模式，这时是可以写数据的。在数据写入完成后，如果要从缓冲区读取数据，就要进行模式的切换，可以调用flip()方法将缓冲区变成读模式，flip为翻转的意思。
 * 在从写模式到读模式的翻转过程中，position和limit属性值会进行调整，具体的规则是：
 * （1）limit属性被设置成写模式时的position值，表示可以读取的最大数据位置。
 * （2）position由原来的写入位置变成新的可读位置，也就是0，表示可以从头开始读。
 * 也就是说flip把limit设为position，再把position设为0。
 * 3.limit属性
 * Buffer类的limit属性表示可以写入或者读取的数据最大上限,其属性值的具体含义也与缓冲区的读写模式有关。
 * 在写模式下，limit属性值的含义为可以写入的数据最大上限。在刚进入写模式时，limit的值会被设置成缓冲区的capacity值，表示可以一直将缓冲区的容量写满。
 * 在读模式下，limit值的含义为最多能从缓冲区读取多少数据。
 * 一般来说，在进行缓冲区操作时是先写入再读取的。当缓冲区写入完成后，就可以开始从Buffer读取数据，调用flip()方法（翻转），这时limit的值也会进行调整。将写模式下的position值设置成读模式下的limit值，也就是说，将之前写入的最大数量作为可以读取数据的上限值。
 * Buffer在翻转时的属性值调整主要涉及position、limit两个属性，下面举一个简单的例子：
 * （1） 首先，创建缓冲区。新创建的缓冲区处于写模式，其position值为0，limit值为最大容量capacity。
 * （2）然后，向缓冲区写数据。每写入一个数据，position向后面移动一个位置，也就是position的值加1。这里假定写入了5个数，当写入完成后，position的值为5。
 * （3）最后，使用flip方法将缓冲区切换到读模式。limit的值会先被设置成写模式时的position值，所以新的limit值是5，表示可以读取数据的最大上限是5。之后调整position值，新的position会被重置为0，表示可以从0开始读。
 * 标记属性：mark（标记）属性
 * 在缓冲区操作过程当中，可以将当前的position值临时存入mark属性中；需要的时候，再从mark中取出暂存的标记值，恢复到position属性中，重新从position位置开始处理。
 * mark方法：将当前的position值临时存入mark属性中
 * reset方法：恢复到position属性中
 */
public class NioBuffer {

    /**
     * 一个缓冲区在新建后处于写模式，position属性（代表写入位置）的值为0，
     * 缓冲区的capacity值是初始化时allocate方法的参数值（这里是100），
     * 而limit最大可写上限值也为allocate方法的初始化参数值。
     */
    @Test
    public void testAllocate() {

        // 参数：指定capacity属性，不能小于0
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        System.out.println("position = " + byteBuffer.position());
        System.out.println("capacity = " + byteBuffer.capacity());
        System.out.println("limit = " + byteBuffer.limit());
    }

    @Test(expected = BufferOverflowException.class)
    public void testPut() {
        IntBuffer buffer = IntBuffer.allocate(5);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        System.out.println("position = " + buffer.position());
        System.out.println("capacity = " + buffer.capacity());
        System.out.println("limit = " + buffer.limit());
        // 转成数数组
        int[] array = buffer.array();
        Arrays.stream(array).forEach(System.out::print);
        // 前面已经写入5个数据了，尝试在写入就会抛出异常: BufferOverflowException
        buffer.put(6);
    }

    /**
     * 向缓冲区写入数据之后，不能直接从缓冲区读取数据，这时缓冲区还处于写模式，如果需要读取数据，要将缓冲区转换成读模式。
     */
    @Test
    public void testFlip() {
        IntBuffer buffer = IntBuffer.allocate(20);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        System.out.println("调用flip之前");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 调用
        buffer.flip();
        System.out.println("调用flip之后");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
    }

    /**
     * get()方法每次从position的位置读取一个数据，并且进行相应的缓冲区属性的调整。
     * 读取操作会改变可读位置position的属性值，而可读上限limit值并不会改变。
     * 在position值和limit值相等时，表示所有数据读取完成，position指向了一个没有数据的元素位置，已经不能再读了，此时再读就会抛出BufferUnderflowException异常。
     */
    @Test
    public void testGet1() {
        IntBuffer buffer = IntBuffer.allocate(20);
        System.out.println("刚初始化buffer：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        System.out.println("调用flip之前：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.flip();
        System.out.println("调用flip之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        int i = buffer.get();
        System.out.println("调用get之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
    }

    /**
     * get还有一个重载方法，可以读取指定索引的数据，但是这个方法不会移动position
     */
    @Test
    public void testGet2() {
        IntBuffer buffer = IntBuffer.allocate(20);
        System.out.println("刚初始化buffer：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        System.out.println("调用flip之前：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.flip();
        System.out.println("调用flip之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        int i = buffer.get(2);
        System.out.println("调用get之后，读取的数据是：" + i);
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
    }

    /**
     * clear()清空 / compact()压缩方法
     * flip是将缓冲区转换成读模式，而这两个它们可以将缓冲区转换为写模式。
     * 1.clear：把极限设为容量，再把位置设为0。
     * 2.compact()方法：删除缓冲区内从0到当前位置position的内容，然后把从当前位置position到极限limit的内容拷贝到0到limit-position的区域内，当前位置position和极限limit的取值也做相应的变化
     * compact: 就是把已经读取出来的空的部分删除，把之前未读取的数据推到buffer的开始位置
     */
    @Test
    public void testCompact() {
        IntBuffer buffer = IntBuffer.allocate(20);
        System.out.println("刚初始化buffer：");
        // 声明的buffer的capacity是20个数据，此时position = 0 ;capacity = 20
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        // 放了5个数据之后，position就从零一定移动到了5
        System.out.println("调用flip之前：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 调用flip之后，准备读取数据，就会把position置为0，limit置为刚刚的5（因为现在只有5个数据可读）
        buffer.flip();
        System.out.println("调用flip之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 调用get读取数据，position就从0移动了1个位置，position = 1了
        int i = buffer.get();
        System.out.println("调用get之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 调用compact之后，删除缓冲区内从0到当前位置position的内容，然后把从当前位置position到极限limit的内容拷贝到0到limit-position的区域内（就会压缩掉刚刚已经读取的出了那个数据的位置），
        // 然后把limit设置为capacity，这样就可以继续写入数据（这里就会从position=4的位置继续写入），所以再次写入100这个数据的之后，position=4变成了position=5
        buffer.compact();
        System.out.println("调用compact之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        buffer.put(100);
        System.out.println("再次写入数据：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
    }

    /**
     * rewind()：倒带（重新读取）
     * 已经读完的数据，如果需要再读一遍，可以调用rewind()方法。rewind()也叫倒带
     * <p>
     * rewind ()方法主要是调整了缓冲区的position属性与mark属性，具体的调整规则如下：
     * 1.position重置为0，所以可以重读缓冲区中的所有数据。
     * 2.limit保持不变，数据量还是一样的，仍然表示能从缓冲区中读取的元素数量。
     * 3.mark被清理，表示之前的临时位置不能再用了。
     * 源码也很是简单：
     * public final Buffer rewind() {
     * position = 0;
     * mark = -1;
     * return this;
     * }
     */
    @Test
    public void testRewind() {
        IntBuffer buffer = IntBuffer.allocate(20);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);

        // 调用flip,准备读取数据
        buffer.flip();
        System.out.println("调用flip之后：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());

        for (int i = 0; i < buffer.limit(); i++) {
            System.out.println(buffer.get());
        }
        // 读取之后的
        System.out.println("读取之后的三个属性信息：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());

        //
        buffer.rewind();
        System.out.println("rewind之后的三个属性信息：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 再次读取
        System.out.println("再次读取：");
        for (int i = 0; i < buffer.limit(); i++) {
            System.out.println(buffer.get());
        }
    }

    /**
     * mark()和reset()
     * 1.mark()方法将当前position的值保存起来放在mark属性中，让mark属性记住这个临时位置
     * 2.reset()方法将mark的值恢复到position中。
     */
    @Test
    public void testMark() {
        IntBuffer buffer = IntBuffer.allocate(5);
        buffer.put(1);
        buffer.put(2);
        buffer.mark();
        buffer.put(3);
        buffer.put(4);
        buffer.put(5);
        // 输出数据
        Arrays.stream(buffer.array()).forEach(System.out::print);
        System.out.println("\nreset之前的三个属性信息：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 调用reset
        buffer.reset();
        System.out.println("reset之后的三个属性信息：");
        System.out.println("position = " + buffer.position() + " ;capacity = " + buffer.capacity() + " ;limit = " + buffer.limit());
        // 再次写入两个数据
        buffer.put(6);
        buffer.put(7);
        // 输出数据
        Arrays.stream(buffer.array()).forEach(System.out::print);
    }
}
