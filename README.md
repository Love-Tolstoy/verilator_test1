# verilator_test1
## 1 已经设计好v(sv)文件，使用命令verilator -cc top.v生成的Cpp库，各种文件存储在obj_dir文件夹中；
## 2 编写好测试文件sim_top.cpp，使用命令verilatot -Wall --trace -cc top.v --exe sim_top.cpp更新Vtop.mk文件；
## 3 使用命令make -C obj_dir -f Vtop.mk Vtop编译生成可执行文件Vtop；
## 4 执行Vtop文件生成波形。
