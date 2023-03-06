#include <stdlib.h>
#include <iostream>             // cpp基础头文件
#include <verilated.h>          // 包含verilator常用API
#include "verilated_vcd_c.h"    // 可选，如果要导出vcd则需要加上
#include "Vtop.h"               // 包含模块的类定义
//#include "Vtop__024unit.h"    // 说包含了类型定义，但这个没有这个文件


#define MAX_SIM_TIME 100    // 仿真总时钟边沿数
vluint64_t sim_time = 0;   // initial 仿真时间，用于计数时间边沿

/*double sc_time_stamp()
{
    return main_time;
}*/
 
int main(int argc, char **argv)
{
    Verilated::commandArgs(argc, argv); 	//用于解析命令行参数，将其存储在verilator内部，供仿真使用
    Vtop *dut = new Vtop;	// 例化转化后的v模块
    //以下四句用于设置波形存储位VCD文件
    Verilated::traceEverOn(true);	// 理解为使能波形追踪和显示
    VerilatedVcdC* my_trace = new VerilatedVcdC;	// 生成波形,创建my_trace实例
    dut -> trace(my_trace, 5);	// 设置采样周期，每隔5个周期记录一次波形
    my_trace -> open("waveform.vcd");	// 打开一个VCD文件，将仿真过程的信号值写入该文件

    while((sim_time <= MAX_SIM_TIME) && (!Verilated::gotFinish()))	// 仿真次数小于最大仿真次数，当仿真运行到$finish$函数时，verilator会将仿真结束标志位置1，这个函数的返回值就为true
    {
        int a = rand() & 1;	// 生成随机输入值，&1的目的是让其的值只有0或1
        int b = rand() & 1;	// 同上
        dut -> a = a;	// 将a赋值给dut的a端口
        dut -> b = b;	// 将b赋值给dut的b端口
        // dut -> clk ^= 1;	// 若是时序逻辑电路，则有时钟，clk与1异或可达到翻转时钟的目的
        // 以上代码都是灵活发挥的
        dut -> eval();	// 更新电路的状态
        // printf("a = %d, b = %d, f = %d\n", a, b, dut->f);	// 在命令行窗口打印输出
        my_trace -> dump(sim_time);	//将所有追踪到的信号写入波形中
        sim_time ++;
    } 
    dut -> final();	// 对Chisel生成的模块进行资源的清理
    my_trace -> close();	// 关闭这个VCD文件
    delete dut;	// 对cpp的dut进行清理，释放其占用的内存空间，防止内存泄漏
    exit(EXIT_SUCCESS);
}

