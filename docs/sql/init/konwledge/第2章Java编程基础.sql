-- ============================================================
-- 《Java 语言程序设计》知识库数据 —— 第 2 章 Java 编程基础
-- 依据《第2章Java编程基础.md》提取，续接第 1 章已有数据的各表最大 id：
--   knowledge_chapter 从 14 起、knowledge_point 从 13 起、knowledge_concept 从 30 起、
--   knowledge_code_snippet 从 7 起、knowledge_content 从 137 起、knowledge_keyword 从 15 起、
--   knowledge_weight_config 从 13 起、knowledge_exercise 从 50 起
-- 说明：源 md 文档自 1992 行起重复出现 2.3~2.5 节部分内容（与 222~960 行逐字重复），
--       本文件以第一副本（含 2.2 节的完整版本）为准提取，重复内容不重复入库。
-- knowledge_difficult_point：已有测试数据
-- ============================================================

INSERT INTO `knowledge_content` (`id`, `chapter_id`, `knowledge_point_id`, `content_type`, `content`, `page`, `page_order`) VALUES
(137, 14, 13, 1, '1. 了解 ASCII 码，理解 Unicode 码、标识符和关键字。', 26, 1),
(138, 14, 13, 1, '2. 理解变量和常量。', 26, 2),
(139, 14, 13, 1, '3. 理解赋值语句与赋值表达式。', 26, 3),
(140, 14, 13, 1, '4. 了解并使用交互式工具 JShell。', 26, 4),
(141, 14, 13, 1, '5. 理解 8 种基本数据类型以及数据类型转换。', 26, 5),
(142, 14, 13, 1, '6. 理解并应用控制台输入/输出。', 26, 6),
(143, 14, 13, 1, '7. 理解并应用算术运算符、增强赋值运算符、关系运算符、逻辑运算符和条件运算符，并能进行混合运算。', 26, 7),
(144, 14, 13, 1, '8. 理解并应用数学函数。', 26, 8),
(145, 14, 13, 1, '9. 理解编程的步骤和算法概念，能用算法描述问题的解决方案；了解编程、算法思维和计算思维的关系。', 26, 9),
(146, 14, 13, 1, '10. 了解并遵守良好的编程规范，理解程序设计风格、注释及命名习惯。', 26, 10),
(147, 14, 13, 1, '11. 理解并避免 3 类程序设计错误。', 26, 11),
(148, 14, 13, 1, '本章内容是学习 Java 语言程序设计的重要基础，包含计算机编程语言中一些常见的编程概念和编程基础知识。本章不仅介绍了 ASCII 码、Unicode 码、标识符、变量、常量、赋值语句、基本数据类型及数据类型转换、运算符及混合运算、控制台输入/输出等基本编程元素，而且介绍了一种新的交互式工具 JShell，方便用户运行和调试代码片段。进一步，介绍了数学函数及其使用以及如何通过编程解决实际问题。针对特定实际问题进行编程求解时，读者应养成先设计算法的良好习惯。算法设计可有效培养读者的算法思维和计算思维。最后，本章还介绍了易读性好的代码应遵循的编程规范以及在编程过程中碰到的 3 类错误。', 26, 12),
(149, 15, NULL, 1, '人类能阅读和理解的字符在计算机中是以 0 和 1 构成的二进制序列来存储的。把字符映射到其二进制序列的过程称为编码（Encoding）。编码是信息从一种形式或格式转换为另一种形式的过程。解码（Decoding）是编码的逆过程。在计算机发展过程中，出现了多种编码方案，如 ASCII 码、Unicode 码、GBK 码（GBK 码可对 2 万多的简繁汉字进行编码）、ISO-8859-1 码（ISO-8859-1 码是单字节编码，向下兼容 ASCII，其编码范围是 0x00~0xFF，0x00~0x7F 之间完全和 ASCII 一致，0x80~0x9F 之间是控制字符，0xA0~0xFF 之间是文字符号）等。早期编程语言如 C、C++ 等都是使用 ASCII 码的。每一种编码方案都定义了如何编码每个字符。Java 语言使用的编码方案是 Unicode 码。', 26, 13),
(150, 16, 14, 1, 'ASCII 码是基于拉丁字母的一套计算机编码系统，它占用一个字节，具体说明见附录Ⅰ。ASCII 是 American Standard Code for Information Interchange（美国信息交换标准代码）的缩写，也被称为“美标”。ASCII 码规定了用 0（0x00）~127（0x7F）的 128 个数字来代表信息的规范编码，包括 33 个控制码、1 个空格码和 94 个形象码。形象码包括英文大小写字母、阿拉伯数字、标点符号等。', 27, 1),
(151, 16, 14, 1, 'Unicode 码的出现是为了解决 ASCII 码只能表示 128 个字符的限制性问题，可支持世界上各种语言的表示。Unicode 码占两个字节，能表示 65536 个字符。然而，世界上各种语言的字符数是远超 65536 个的。因此，Unicode 标准引入了补充字符集（Supplementary Character），可支持 1 112 064 个字符。为了避免引入复杂性，本书仅考虑原来占用两个字节的 Unicode 码。一个 Unicode 字符使用两个字节，用 \\u 开头的 4 位十六进制数表示，范围为 \\u0000~\\uFFFF。例如，汉字“中国”对应的 Unicode 码分别是 \\u4e2d、\\u56fd，希腊字母 α、β、γ 的 Unicode 码分别是 \\u03b1、\\u03b2、\\u03b3。', 27, 2),
(152, 16, 14, 1, 'Unicode 码包含 ASCII 码，Unicode 码的前 128 个字符（\\u0000~\\u007F）与 ASCII 码的 128 个字符一致。', 27, 3),
(153, 16, 14, 1, '下面通过一个示例，演示 Unicode 码的使用，程序如程序清单 2-1 所示。', 27, 4),
(154, 16, 14, 1, '该程序的执行结果如下所示：', 27, 5),
(155, 17, 15, 1, '标识符是为了命名程序中的类、方法、变量等元素。如程序清单 2-1 所示，类名 TestUnicode、方法名 main、变量名 letter1、letter2、letter3、letter4 等都是标识符。根据 Java 语言规范的定义，标识符是 Java 字母和 Java 数字组成的长度无限制的字符序列，字符序列的开头必须是 Java 字母。标识符的命名满足如下要求。', 28, 1),
(156, 17, 15, 1, '(1)由字母、数字构成，长度不限。', 28, 2),
(157, 17, 15, 1, '(2)标识符以字母开头，不能以数字开头。', 28, 3),
(158, 17, 15, 1, '(3)标识符不能使用保留关键字（Reserved Keyword），也不能是字面常量 true、false、null。', 28, 4),
(159, 17, 15, 1, '(4)标识符可以是上下文关键字（Contextual Keyword）。上下文关键字的字符序列是作为关键字还是标识符，取决于这个字符序列在程序中出现的位置。', 28, 5),
(160, 17, 15, 1, 'Java 字母可以从整个 Unicode 字符集中提取，该字符集支持当今世界上使用的大多数书写脚本，包括中文、日文和韩文等大型字符集。这个特点使得世界各地的程序员可以在其编写的程序中使用自己的母语来命名标识符。', 28, 6),
(161, 17, 15, 1, '具体来说，Java 字母不仅包括大写和小写 ASCII 拉丁英文字母，即 A~Z（\\u0041~\\u005a）和 a~z（\\u0061~\\u007a），而且包括汉语中的汉字、希腊字母、日文、韩文、俄文以及其他许多语言中的文字。另外，Java 字母还包括 ASCII 美元符号（$ 或 \\u0024）和下画线（_ 或 \\u005f）。但是，Java 字母不包括标点符号、空格及除美元符号（$）、下画线（_）外的特殊字符。', 28, 7),
(162, 17, 15, 1, '注意：美元符号 $ 只在机器生成的源代码中使用，不能用来命名标识符。虽然程序员在编程中可使用美元符号 $ 来命名标识符，不会产生编译错误，但是不建议程序员使用。下画线（_）可用于由两个或多个字符组成的标识符中，但由于单个下画线是关键字，因而不能用作单字符标识符。', 28, 8),
(163, 17, 15, 1, '表 2-1 列举了一些合法的标识符。', 28, 9),
(164, 17, 15, 1, '表 2-1　合法的标识符示例\n| 标识符 | 标识符 |\n| --- | --- |\n| TestJava | hello |\n| world | test |\n| first | word |\n| 利率 | a |\n| $1 | Test$1 |\n| interstate | 唐 |', 28, 10),
(165, 17, 15, 1, '表 2-2 中列举了一些不合法的标识符示例。如果在 Java 源程序中使用了不合法的标识符，则编译器在编译时会报错。', 28, 11),
(166, 17, 15, 1, '表 2-2　不合法的标识符示例\n| 不合法的标识符 | 原因 |\n| --- | --- |\n| Test Java | 标识符中不能有空格 |\n| 8test | 标识符不能以数字开头 |\n| 可以? | ?既不是 Java 字母，也不是 Java 数字 |\n| Java Python | 既不是 Java 字母，也不是 Java 数字 |\n| A+B | +既不是 Java 字母，也不是 Java 数字 |\n| A#B | #既不是 Java 字母，也不是 Java 数字 |\n| true | true 是字面常量，不允许被用作标识符 |', 29, 1),
(167, 18, 16, 1, '关键字是具有特定用途或特定意义的词。关键字可分为保留关键字和上下文关键字。保留关键字是由 ASCII 字符组成的 51 个字符序列，不能用作标识符，它们分别为 abstract、continue、for、new、switch、assert、default、if、package、synchronized、boolean、do、goto、private、this、break、double、implements、protected、throw、byte、else、import、public、throws、case、enum、instanceof、return、transient、catch、extends、int、short、try、char、final、interface、static、void、class、finally、long、strictfp、volatile、const、float、native、super、while、_（underscore）。在保留关键字中，关键字 const、goto 是来自 C++ 语言的保留关键字，目前在 Java 语言中没有被使用。但是，Java 编译器能够识别它们，并产生编译错误信息。因此，Java 程序中也不能出现 const、goto。关键字 strictfp 过时了，在新的 Java 程序中也不允许被使用。', 29, 2),
(168, 18, 16, 1, '另外，上下文关键字是由 ASCII 字符组成的 16 个字符序列，分别为 exports、opens、requires、uses、module、permits、sealed、var、non-sealed、provides、to、with、open、record、transitive、yield。这 16 个字符序列可以被解释为上下文关键字或其他标记（即可使用的标识符），具体取决于它们出现的上下文。为了避免引入复杂性，初学者尽量避免使用上下文关键字作为标识符。', 29, 3),
(169, 20, 17, 1, '变量是指在程序中可以被改变的量，具有 4 个基本要素：变量名、数据类型、存储单元和变量值。变量名用来指代变量，可以使用合法的标识符来命名。变量的数据类型可以是 Java 语言支持的各种数据类型。变量的存储单元是用来在内存中存储变量值的，不同的数据类型对存储单元有不同的要求。例如，如果一个变量的数据类型是 int（整数类型），将占用一个具有 4 个字节的存储单元；如果变量的数据类型是 double（双精度浮点型），将占用一个具有 8 个字节的存储单元。变量的变量值是变量在程序运行某个时刻的取值，被存储于该变量的存储单元中。如果一个变量的数据类型是基本数据类型，那么在存储单元中存储的是具体的数值或布尔值。如果一个变量的数据类型是引用类型，那么在存储单元中存储的是引用值。引用类型与引用值将在后文详述。', 29, 4),
(170, 20, 17, 1, '变量在使用之前，必须先进行声明和初始化。变量声明可以告知 Java 编译器一个变量的变量名和数据类型，编译器可以为该变量分配合适的内存空间。变量声明的语法如下：', 29, 5),
(171, 20, 17, 1, '数据类型　变量名；', 29, 6),
(172, 20, 17, 1, '下面是一些变量声明的例子：', 29, 7),
(173, 20, 17, 1, '上面的例子声明了 3 个基本数据类型变量和 1 个引用数据类型变量。后续内容会根据各种数据类型进行详述。', 30, 1),
(174, 20, 17, 1, '如果几个变量是同一类型，可以一起声明它们，语法形式如下：', 30, 2),
(175, 20, 17, 1, '数据类型　变量名1、变量名2，…，变量名n；', 30, 3),
(176, 20, 17, 1, '变量之间用逗号分隔。例如：', 30, 4),
(177, 20, 17, 1, '通常，变量具有初始值，而且变量在使用前必须具有初始值。变量可以在声明后被初始化，也可以在声明时一同初始化，还可以被 Java 编译器默认初始化（默认初始化规则后续再讲）。声明一个变量同时初始化的语法形式如下：', 30, 5),
(178, 20, 17, 1, '数据类型　变量名=初值；', 30, 6),
(179, 20, 17, 1, '例如：', 30, 7),
(180, 20, 17, 1, '等同于下面两条语句：', 30, 8),
(181, 20, 17, 1, '如果需要同时声明和初始化同一类型的多个变量，可采用如下语法形式：', 30, 9),
(182, 20, 17, 1, '数据类型　变量名1=初值1，变量名2=初值2，…，变量名n=初值n；', 30, 10),
(183, 20, 17, 1, '例如：', 30, 11),
(184, 21, 18, 1, '常量是在程序执行过程中不会发生改变的数据。在 Java 语言中，常量是由 final 修饰的一个合法标识符。声明常量的语法形式如下：', 30, 12),
(185, 21, 18, 1, 'final 数据类型　常量名=值;', 31, 1),
(186, 21, 18, 1, '在声明常量时，必须在同一条语句中对常量进行声明和初始化。final 是声明常量的 Java 关键字。常量名是一个合法标识符，常常采用大写英文字母，例如，数学常量 π 可以如下定义：', 31, 2),
(187, 21, 18, 1, '下面通过一个计算圆面积的示例使用数学常量 π，如程序清单 2-2 所示。', 31, 3),
(188, 21, 18, 1, '运行结果如下：', 31, 4),
(189, 21, 18, 1, '程序清单 2-2 第 3 行定义了常量 PI，第 5 行、第 7 行的表达式 (PI*5*5)、(PI*7.5*7.5) 分别计算了两个圆的面积。', 31, 5),
(190, 21, 18, 1, '如果认为计算的精度不够高，想让 PI 的值保留到小数点后 5 位，那么只需要修改第 3 行的常量 PI，其他的代码不用修改。', 31, 6),
(191, 21, 18, 1, '一般来说，常量的使用有 3 个好处。第一，如果需要修改常量值，只需要在源程序中定义常量的地方进行修改，其他的代码无须改变，如 PI 的值。第二，当一个值被重复使用时，这个值可以被定义为常量，不必重复输入这个值。例如，在计算多个圆的面积和周长时，均需要用到数值 3.14159，那么就可以定义一个常量 PI 表示 3.14159，避免重复输入。第三，定义常量能提升程序的易读性，这是因为常量的名字相较于值而言，更容易被描述。', 31, 7),
(192, 22, 19, 1, '变量（Variable）在声明之后，可以使用赋值语句（Assignment Statement）对其赋值（赋初值或修改其值）。赋值语句使用赋值运算符“=”（等号），赋值语句的语法如下所示：', 31, 8),
(193, 22, 19, 1, '变量=表达式；', 31, 9),
(194, 22, 19, 1, '表达式（Expression）是包含变量、值和运算符的一次计算，通过计算可以得到一个新值。赋值运算符是一个从右向左结合的运算符，赋值语句右侧的表达式先被求值，再被赋给左侧的变量。左侧变量的数据类型与右侧表达式值的数据类型应兼容，兼容的含义是数据类型要么一致，要么右侧表达式值的数据类型能将隐式类型转换为左侧变量的数据类型，数据类型转换的内容将在后文讲解。为了简便，在下面示例中的赋值，左侧变量的数据类型与右侧表达式值的数据类型都是一致的。例如，下面的一段代码：', 31, 10),
(195, 22, 19, 1, '在上面的例子中，变量出现在赋值语句两边都是可以的。当一个变量具有初值后，还可以参与赋值语句右侧表达式的计算，例如：', 32, 1),
(196, 22, 19, 1, '赋值语句去掉分号后就是赋值表达式（Assignment Expression），赋值表达式求值的结果是赋值运算符左侧变量的值。例如，下面的语句：', 32, 2),
(197, 22, 19, 1, '赋值表达式 k=k+5 的值是左侧变量 k 的值，等于 10，因此，上面的输出语句会输出 10。上面的输出语句等价于下面两条语句：', 32, 3),
(198, 22, 19, 1, '如果用一个值给多个同类型变量赋值，也是可以的，其语法形式如下：', 32, 4),
(199, 22, 19, 1, '变量名1=变量名2=…=变量名n=值；', 32, 5),
(200, 22, 19, 1, '这种赋值方式也称为链式赋值。例如，i、j、k 均为整型变量，可以如下进行赋值：', 32, 6),
(201, 22, 19, 1, '根据赋值运算符自右向左结合的特点，上述链式赋值语句等价于如下语句：', 32, 7),
(202, 22, 19, 1, '首先将值 5 赋值给变量 k，再把变量 k 的值赋给变量 j，最后把变量 j 的值赋值给变量 i。', 32, 8),
(203, 23, 20, 1, 'JShell（Java Shell）工具是自 Java SE 9 开始引入的一个新特性——命令行交互工具，以 REPL（Read-Eval-Print Loop，读取——计算——打印循环）方式交互式评估 Java 语言的声明、语句和表达式。JShell 支持单个表达式或语句的执行，并立即查看结果，不需要编写完整的类。', 32, 9),
(204, 23, 20, 1, 'JShell 提供了一种以交互方式评估 Java 编程语言的声明、语句和表达式的方法，使学习 Java 语言、探索不熟悉的代码和 API 以及创建复杂代码原型变得更加容易。JShell 可接受 Java 语句、变量定义、方法定义、类定义、导入语句和表达式。这部分输入的代码被称为代码段（Snippet）。', 32, 10),
(205, 23, 20, 1, '在代码段被输入后，JShell 对其进行计算，并立即提供反馈。根据输入的代码段和选择的反馈模式，反馈信息包括从操作的结果和解释到无任何信息。不管反馈模式如何，都会描述错误。通过使用详细模式（Verbose Mode），使得用户在学习 JShell 工具时可获得更多反馈。', 32, 11);

INSERT INTO `knowledge_concept` (`id`, `chapter_id`, `name`, `definition`, `difficulty`, `page`, `page_order`) VALUES
(30, 16, '编码（Encoding）', '把字符映射到其二进制序列的过程称为编码（Encoding）。编码是信息从一种形式或格式转换为另一种形式的过程。', 1, 26, 1),
(31, 16, '解码（Decoding）', '解码（Decoding）是编码的逆过程。', 1, 26, 2),
(32, 16, 'ASCII 码', 'ASCII 码是基于拉丁字母的一套计算机编码系统，它占用一个字节。ASCII 是 American Standard Code for Information Interchange（美国信息交换标准代码）的缩写，也被称为“美标”。ASCII 码规定了用 0（0x00）~127（0x7F）的 128 个数字来代表信息的规范编码，包括 33 个控制码、1 个空格码和 94 个形象码。形象码包括英文大小写字母、阿拉伯数字、标点符号等。', 1, 27, 1),
(33, 16, 'Unicode 码', 'Unicode 码的出现是为了解决 ASCII 码只能表示 128 个字符的限制性问题，可支持世界上各种语言的表示。Unicode 码占两个字节，能表示 65536 个字符。一个 Unicode 字符使用两个字节，用 \\u 开头的 4 位十六进制数表示，范围为 \\u0000~\\uFFFF。Unicode 码包含 ASCII 码，Unicode 码的前 128 个字符（\\u0000~\\u007F）与 ASCII 码的 128 个字符一致。', 1, 27, 2),
(34, 16, '补充字符集（Supplementary Character）', '世界上各种语言的字符数远超 65536 个，因此 Unicode 标准引入了补充字符集（Supplementary Character），可支持 1 112 064 个字符。', 1, 27, 3),
(35, 17, '标识符', '标识符是为了命名程序中的类、方法、变量等元素。根据 Java 语言规范的定义，标识符是 Java 字母和 Java 数字组成的长度无限制的字符序列，字符序列的开头必须是 Java 字母。', 1, 28, 1),
(36, 17, 'Java 字母', 'Java 字母可以从整个 Unicode 字符集中提取。Java 字母不仅包括大写和小写 ASCII 拉丁英文字母（A~Z 和 a~z），而且包括汉语中的汉字、希腊字母、日文、韩文、俄文以及其他许多语言中的文字，还包括 ASCII 美元符号（$）和下画线（_），但不包括标点符号、空格及除美元符号（$）、下画线（_）外的特殊字符。', 1, 28, 2),
(37, 18, '保留关键字（Reserved Keyword）', '保留关键字是由 ASCII 字符组成的 51 个字符序列，不能用作标识符。其中，关键字 const、goto 是来自 C++ 语言的保留关键字，目前在 Java 语言中没有被使用，但是 Java 编译器能够识别它们，并产生编译错误信息。关键字 strictfp 过时了，在新的 Java 程序中也不允许被使用。', 1, 29, 1),
(38, 18, '上下文关键字（Contextual Keyword）', '上下文关键字是由 ASCII 字符组成的 16 个字符序列（exports、opens、requires、uses、module、permits、sealed、var、non-sealed、provides、to、with、open、record、transitive、yield），可以被解释为上下文关键字或其他标记（即可使用的标识符），具体取决于它们出现的上下文。', 1, 29, 2),
(39, 21, '常量', '常量是在程序执行过程中不会发生改变的数据。在 Java 语言中，常量是由 final 修饰的一个合法标识符。声明常量时，必须在同一条语句中对常量进行声明和初始化。', 1, 30, 1),
(40, 22, '赋值语句（Assignment Statement）', '变量（Variable）在声明之后，可以使用赋值语句（Assignment Statement）对其赋值（赋初值或修改其值）。赋值语句使用赋值运算符“=”（等号），语法为：变量=表达式；', 1, 31, 1),
(41, 22, '表达式（Expression）', '表达式（Expression）是包含变量、值和运算符的一次计算，通过计算可以得到一个新值。', 1, 31, 2),
(42, 22, '赋值表达式（Assignment Expression）', '赋值语句去掉分号后就是赋值表达式（Assignment Expression），赋值表达式求值的结果是赋值运算符左侧变量的值。', 1, 32, 1),
(43, 22, '链式赋值', '用一个值给多个同类型变量赋值的方式称为链式赋值，语法形式为：变量名1=变量名2=…=变量名n=值；根据赋值运算符自右向左结合的特点，从最右侧的赋值开始依次进行。', 1, 32, 2),
(44, 23, '代码段（Snippet）', 'JShell 可接受 Java 语句、变量定义、方法定义、类定义、导入语句和表达式，这部分输入的代码被称为代码段（Snippet）。', 1, 32, 3),
(45, 24, '基本数据类型', 'Java 语言的数据类型分为基本数据类型和引用数据类型两大类。基本数据类型又可分为数值类型和布尔类型，数值类型又可分为整数类型和浮点类型。Java 语言的基本数据类型有 7 种，每种基本数据类型数据的取值范围和占用的存储空间都是固定的，不依赖于具体的计算机。', 1, 36, 1),
(46, 24, '字面值（Literal）', '字面值是直接出现在程序中的常量值，每种基本数据类型都有其对应的字面值。', 1, 36, 2),
(47, 25, '整型字面值', '整型字面值是可以赋值给一个整数类型的常量值，默认为 int 型（-2³¹~2³¹-1）。表示长整型需在字面值后加 L 或 l（推荐使用 L）。Java 也支持二进制（前置 0b 或 0B）、八进制（前置 0）、十六进制（前置 0x 或 0X）整型字面值，并允许用下画线分隔数字（不能位于开头和结尾处）。', 1, 37, 1),
(48, 26, '浮点型字面值', '浮点型字面值是带小数点的，默认为 double 型；表示 float 型需在字面值后加 f 或 F，也可加 d 或 D 明确表示 double 型。可用科学记数法表示，下画线也可用作数字之间的分隔符（小数点前后不允许）。', 1, 38, 1),
(49, 26, 'IEEE 754', 'IEEE 754 是用于在计算机上表示浮点数的标准。Java 语言对 float 类型采用 32 位的 IEEE 754，对 double 类型采用 64 位的 IEEE 754。自 Java SE 15 开始，使用 2019 版 IEEE 754 标准。', 1, 38, 2),
(50, 26, 'Infinity 与 NaN', 'Java 语言用于表示溢出和出错情况的 3 个特殊的浮点数值是正无穷大（Infinity）、负无穷大（-Infinity）和 NaN（Not a Number，不是一个数字），通过使用常量表达式（如 1f/0f、-1d/0d、0.0/0）可以表示无限（Infinity）、NaN，而不会产生错误。', 1, 38, 3),
(51, 26, '科学记数法', '浮点型字面值可以用 a×10ᵇ 形式的科学记数法表示，去掉乘号“×”，10 的整数次幂由字母 e 或 E 后接一个整数（可带正负号）表示。', 1, 39, 1),
(52, 27, '字符类型（char）', '字符数据类型 char 表示单个字符，占用两个字节，本质上是一个无符号整数，表示 0~65535 的整数，对应字符的 Unicode 编码为 \\u0000~\\uffff。', 1, 40, 1),
(53, 27, '字符字面值', '字符字面值是用单引号括住的单个字符或是一个转义序列（Escape Sequence）。用双引号括起来的单个字符表示的是字符串字面值。', 1, 40, 2),
(54, 27, '转义字符（Escape Character）', '反斜杠 \\ 是转义字符，用于去掉其后所跟字符的特殊含义，把特殊字符转变为普通字符。', 1, 40, 3),
(55, 27, '转义序列（Escape Sequence）', '转义序列由反斜杠（\\）后面加上一个字符或一些数位组成，转义序列中的符号是被整体解释的，一个转义序列被作为单个字符处理。', 1, 41, 1),
(56, 28, '布尔类型（boolean）', '布尔类型是指仅有值为 true 或 false 的数据类型，使用关键字 boolean 声明，占用 1 个字节，表达了逻辑上的真（true）与假（false）。', 1, 42, 1),
(57, 29, '隐式类型转换', '在 Java 语言中，总是可以将一种数值类型的数值直接赋值给支持更大范围数值类型的变量，这种类型转换称为隐式类型转换，不会造成精度损失。', 1, 42, 2),
(58, 29, '显式类型转换（强制类型转换）', '把一个范围大的数值类型数值赋给一个范围小的数值类型变量时，必须进行显式类型转换，也称为强制类型转换，语法形式为：目标数据类型 变量名=(目标数据类型)表达式；浮点型数值显式转换为整型数值时，直接被截去小数部分。', 1, 43, 1),
(59, 30, 'Scanner 类', 'Scanner 类是 java.util 包中的一个类。为了从键盘输入数据，需要使用 Scanner 类创建一个 Scanner 对象，以读取来自 System.in 的输入，该对象能从标准输入设备（键盘）获取信息，通过调用 nextXXX 方法获取输入数据。', 1, 44, 1),
(60, 30, '导入语句（Import Statement）', '导入语句是使用关键字 import 将其他包中的类导入到程序中的语句。例如，Scanner 类在 java.util 包中，使用时需要在程序中通过导入语句将其导入。', 1, 45, 1),
(61, 30, '特定导入（Specific Import）', '特定导入是在导入语句中指定单个具体的类。例如，import java.util.Scanner; 仅仅导入了一个类 Scanner。', 1, 46, 1),
(62, 30, '通配符导入（Wildcard Import）', '通配符导入是通过使用星号 * 作为通配符（Wildcard Character），导入一个包中所有的类。例如，import java.util.*; 导入包 java.util 中的所有类。', 1, 46, 2),
(63, 31, '格式化输出（printf）', 'Java 语言提供了 System.out.printf() 方法在控制台上以某种指定的格式输出信息，printf 中的 f 表示格式（Format）。调用形式为 System.out.printf(格式字符串, 输出项 1, 输出项 2, ……, 输出项 k)，其中格式字符串由字符串子串和格式限定符构成。', 1, 48, 1),
(64, 31, '格式限定符（Format Specifier）', '格式限定符是以百分号 % 开头的转换码，用于指定对应的输出项该如何显示，输出项可以是数值、字符、布尔值或字符串等。常用的简单格式限定符有 %b（布尔值）、%c（字符）、%d（十进制整数）、%f（浮点数）、%e（科学记数法形式的数）、%s（字符串），且格式限定符与输出项必须在顺序、数量和数据类型上匹配。', 1, 49, 2),
(65, 31, '域宽（Field Width）', '域宽是格式限定符中指定的输出项总宽度，如 %6.2f 中的 6。域宽可以是正数，也可以是负数：正数表示右对齐，负数表示左对齐；域宽低于输出项的实际宽度时，按输出项的实际宽度输出。', 1, 49, 3),
(66, 31, '精度（Precision）', '精度是浮点数格式限定符中小数点后指定的位数，如 %6.2f 表示小数点后保留 2 位。%f 在没有指定精度时，默认输出小数点后 6 位。', 1, 49, 4),
(67, 33, '算术运算符', '算术运算符包括加减运算符、乘除运算符、取余运算符、自增自减运算符等，均为数值型操作数运算。加减运算符优先级相同；乘除运算符优先级相同且高于加减；取余运算符优先级与乘除相同。+、- 还可作为一元运算符表示正号、负号，优先级高于乘除运算。', 1, 52, 1),
(68, 33, '取余运算符（模运算）', '取余运算符是百分号 %，取余运算也称为求余运算、模运算、取模运算，是二元运算符，运算结果是执行除法后得到的余数。只有当被除数是负数时，余数才是负的。字符参与取余运算时，先被转换为对应的整数再参与运算。', 1, 52, 2),
(69, 33, '自增自减运算符', '自增运算符是 ++，自减运算符是 --，均为一元运算符，操作数是数值型变量，分前置与后置两种：后置（如 v++）在表达式中使用 v 原来的值，前置（如 ++v）使用 v 新的值。自增、自减运算符的优先级高于乘除运算。', 1, 53, 1),
(70, 33, '算术混合运算类型转换规则', '两个不同类型的操作数进行二元运算时的数据类型转换规则：（1）有 double 则都转 double；（2）最高精度为 float 则都转 float；（3）最高精度为 long 则都转 long；（4）为 int 或低于 int 则都转 int；运算结果与转换后的类型一致。', 1, 55, 1),
(71, 34, '增强赋值运算符', '运算符 +、-、*、/、% 与赋值运算符结合形成的运算符（+=、-=、*=、/=、%=），优先级与赋值运算符一样且从右向左结合；x1 op= x2 可等价转换为 x1 = (T)(x1 op x2)，支持自动的强制类型转换形式。', 1, 56, 1),
(72, 35, '关系运算符', '关系运算符（Relation Operator）也称为比较运算符（Comparison Operator），是二元运算符，用于比较两个值的关系，运算结果是布尔类型的值 true 或 false。Java 提供 >、>=、<、<=、==、!= 共 6 种关系运算符。', 1, 57, 1),
(73, 36, '逻辑运算符', '逻辑运算符（Logical Operator）也称为布尔运算符（Boolean Operator），用于计算布尔值，运算结果产生新的布尔值（true 或 false），有 !（非）、&&（与）、||（或）、^（异或）4 种。', 1, 57, 2),
(74, 36, '短路运算符（懒惰运算符）', '&& 和 || 被称为短路运算符或懒惰运算符：&& 的第一个操作数为 false 时不再考虑第二个操作数；|| 的第一个操作数为 true 时不再考虑第二个操作数，直接给出运算结果。', 1, 58, 1),
(75, 37, '条件运算符', 'Java 提供的三元条件运算符“? :”，一般形式为“布尔表达式 1 ? 表达式 2 : 表达式 3”：表达式 1 为 true 时整个条件运算表达式的值为表达式 2 的值，否则为表达式 3 的值。', 1, 58, 2),
(76, 38, '运算符优先级', '运算符优先级规则定义了运算符的先后次序：圆括号具有最高优先级；算术运算符的优先级高于关系运算符，关系运算符的优先级高于逻辑运算符（除了逻辑非运算符 !），逻辑运算符的优先级高于赋值运算符。', 1, 59, 1),
(77, 38, '结合方向', '优先级相同的运算符在一起运算时，由结合方向决定运算符的先后次序。对于二元运算符而言，除了赋值运算符，所有二元运算符都是左结合的（从左向右）；赋值运算符与增强赋值运算符从右向左结合。', 1, 59, 2),
(78, 39, 'Math 类', 'Java 语言在 java.lang 包（Java 语言基础包，隐式导入到程序中）中提供的数学类，在程序中可以直接使用。Math 类提供许多常用的数学函数方法，分为三角函数方法、指数函数方法和服务方法 3 类，还提供两个很有用的 double 型常量：PI（圆周率 π，约等于 3.1415926）和 E（自然对数的底，约等于 2.71828）。', 1, 60, 1),
(79, 40, '三角函数方法', 'Math 类提供的一系列计算三角函数的方法，包括 sin、cos、tan、asin、acos、atan、toRadians、toDegrees，这些方法的返回值均为 double 类型，也都是 public 方法。asin 和 atan 的返回值位于 -π/2~π/2 之间，acos 的返回值位于 0~π 之间。', 1, 60, 1),
(80, 41, '指数函数方法', 'Math 类提供的 5 个与指数函数有关的方法：exp（返回 e 的 a 次幂）、log（返回 a 的自然对数）、log10（返回 a 的以 10 为底的对数）、sqrt（对于 a≥0 的数字返回 a 的平方根，对于 a<0 的数字或 NaN 返回 NaN）、pow（返回 a 的 b 次幂）。', 1, 62, 1),
(81, 42, '服务方法', 'Math 类提供的服务方法，包括取整、求最大值、求最小值、求绝对值及随机方法。', 1, 63, 1),
(82, 42, '取整方法', 'Math 类的取整方法包括 ceil（向上取整为最接近的整数）、floor（向下取整为最接近的整数）、rint（取整为最接近的整数，如果与相近的两个整数距离相等则返回偶数整数）、round（四舍五入为最接近的整数，float 参数返回 int 类型，double 参数返回 long 类型）。', 1, 63, 2),
(83, 42, '随机方法', 'random 方法（public static double random()）生成一个大于等于 0.0 且小于 1.0 的 double 类型随机数。假设 a、b 为两个浮点数且 a≤b，生成 a~b（包含 a、不含 b）的随机数，表达式为 a + Math.random()*(b-a)；假设 a、b 为两个整数且 a≤b，生成 a~b（包含 a、b）的随机整数，表达式为 a + (int)(Math.random()*(b-a+1))。', 1, 64, 3),
(84, 44, '编程', '编写程序的简称，是使用特定计算机语言编写计算机程序，以控制计算机执行特定任务或解决特定问题。编程解决某个特定实际问题一般需要两个步骤：第一步，对问题进行求解，设计解决问题的策略（算法）；第二步，应用某种程序设计语言把这种策略转换成可以在现代通用计算机上执行的程序。', 1, 64, 1),
(85, 44, '程序流程图', '描述算法的图形化方式。基本符号包括：椭圆或圆角矩形，表示开始或结束；平行四边形，表示一个数据输入或数据输出的处理步骤；矩形，表示一个单独的处理步骤；菱形，表示一项判定，引出两个分支；箭头，表示一个过程的流程方向。', 1, 65, 2),
(86, 44, '海伦公式', '根据三角形三条边的边长计算三角形面积的公式：area=√p(p-a)(p-b)(p-c)，其中 a、b、c 为三角形的三条边，p 为三角形周长的一半，即 p=(a+b+c)/2。', 1, 65, 3),
(87, 45, '算法', '特定问题求解步骤的准确而完整的描述，是解决问题的有限指令序列，其中每条指令表示计算机的一个或多个操作。算法可以用自然语言、伪码（自然语言与一些程序设计代码的混合）、程序流程图来描述。', 1, 66, 1),
(88, 45, '算法的 5 个特性', '有穷性（算法必须在执行有限个步骤之后结束，且每个步骤在有限时间内完成）、确定性（算法的每个步骤必须有确切的定义）、可行性（算法的所有步骤均可以分解为基本的、可执行的操作）、有输入（一个算法有零个或多个输入）、有输出（一个算法有一个或多个输出）。', 1, 67, 2),
(89, 45, '计算思维', '由周以真教授于 2006 年提出的概念，该定义被国际学术界广泛采用。计算思维是运用计算机科学的基础概念进行问题求解、系统设计以及人类行为理解等涵盖计算机科学之广度的一系列思维活动。计算思维源于计算机科学，又高于计算机科学。', 1, 67, 3),
(90, 45, '算法思维', '构建和理解与算法有关的思维模式，包括算法权衡、系统模型设计、算法策略选择、约简等。', 1, 67, 4),
(91, 47, '程序设计风格（Program Style）', '程序设计风格是对程序代码排版所设计出来的外观样式。', 1, 67, 5),
(92, 47, '缩进（Indentation）', '在源代码编辑器中，缩进是代码与左边边界之间的距离。', 1, 68, 1),
(93, 47, '代码块（Block）', '代码块是由花括号括起来的一组语句。', 1, 68, 2),
(94, 47, '行尾风格', '行尾风格将起始花括号置于一行的末位，结束花括号采用缩进方式表示结构关系。', 1, 68, 3),
(95, 47, '次行风格', '次行风格将起始花括号置于新的一行，并采用适当缩进，与结束花括号垂直对齐。', 1, 68, 4),
(96, 48, 'Java 文档注释（Javadoc Comment）', 'Java 文档注释是用来生成应用程序接口（API）文档的。Java 文档注释以 /** 开始，并以 */ 结束，通过使用"javadoc"命令将文档提取到一个 HTML 文件中。', 1, 68, 5),
(97, 49, '驼峰命名法', '把所有的单词连接在一起，第一个单词的首字母采用小写或大写，而后面的每个单词的首字母采用大写。', 1, 73, 1),
(98, 49, '下划线命名法', '第一个单词的首字母采用小写或大写，而后面的每个单词均采用小写且用一个下划线连接。', 1, 73, 2),
(99, 51, '编译错误（Compile Error）', '在编译过程中由编译器检测到的错误，一般是编译器检测到的语法错误（Syntax Error），因此编译错误也被称为语法错误。', 1, 74, 1),
(100, 52, '运行时错误（Runtime Error）', '程序在运行时出现错误，导致程序异常终止。一个典型的运行时错误就是将 0 用作除数的情况。', 1, 75, 1),
(101, 53, '逻辑错误（Logic Error）', '程序没有正确达到预期的功能。这种错误发生的原因有很多种，有些是错误的编码引起的，有些是对需求的理解发生偏差引起的。', 1, 75, 2);

INSERT INTO `knowledge_code_snippet` (`id`, `chapter_id`, `title`, `code`, `is_key_example`, `page`, `page_order`) VALUES
(7, 16, '程序清单 2-1　TestUnicode.java', '1  public class TestUnicode {
2      public static void main(String[] args) {
3          char letter1 = \'中\';
4          char letter2 = \'\\u4e2d\';
5          char letter3 = \'α\';
6          char letter4 = \'\\u03b1\';
7          System.out.println(letter1 + ": " + letter2);
8          System.out.println(letter3 + ": " + letter4);
9
10
11         //计算字符的十六进制值
12         System.out.println(Integer.toHexString(letter3));
13         System.out.println(Integer.toHexString(\'中\'));
14     }
15 }', 1, 27, 1),
(8, 16, '程序清单 2-1 运行结果', '中: 中
α: α
3b1
4e2d', 0, 27, 2),
(9, 20, '变量声明示例', 'char letter1;        //声明 letter1 是一个字符型变量
int count_people;    //声明 count_people 是一个整数类型变量
double 利率;         //声明“利率”是一个双精度浮点型变量
String cityName;     //声明 cityName 是一个字符串引用变量', 0, 29, 3),
(10, 20, '同类型的多个变量一起声明示例', 'char letter1, letter2, letter3;   //声明 letter1、letter2、letter3 是字符型变量', 0, 30, 4),
(11, 20, '变量声明并初始化示例', 'char letter1 = \'中\';   //声明一个字符变量 letter1，其初值为‘中’', 0, 30, 5),
(12, 20, '声明与初始化两条语句示例', 'char letter1;      //声明 letter1 是一个字符型变量
letter1 = \'中\';    //字符变量 letter1 赋值为‘中’', 0, 30, 6),
(13, 20, '同一类型多个变量声明并初始化示例', 'int i = 2, j = 3, k = 5;   //声明和初始化 3 个整型变量 i、j、k，它们的值分别是 2、3、5', 0, 30, 7),
(14, 21, '常量 PI 定义示例', 'final double PI = 3.14;   // double 表示双精度浮点型，PI 是常量名', 0, 31, 8),
(15, 21, '程序清单 2-2　CalcuCircleArea.java', '1  public class CalcuCircleArea {
2      public static void main(String[] args) {
3          final double PI = 3.14;
4          System.out.println("一个圆的半径是" + 5 +
5                  ",其面积是" + (PI * 5 * 5));
6          System.out.println("一个圆的半径是" + 7.5 +
7                  ",其面积是" + (PI * 7.5 * 7.5));
8      }
9  }', 1, 31, 9),
(16, 21, '程序清单 2-2 运行结果', '一个圆的半径是5，其面积是78.5
一个圆的半径是7.5，其面积是176.625', 0, 31, 10),
(17, 22, '赋值语句示例', 'int x = 1;            //将值 1 赋给整型变量 x
int y = x + 1;        //先计算表达式 x+1，得到值 2，再把值 2 赋给整型变量 y
int z = x + y + 10;   //先计算表达式 x+y+10，得到值 12，再把值 12 赋给整型变量 z', 0, 31, 11),
(18, 22, '变量参与右侧表达式计算示例', 'int k = 5;    //将值 5 赋给整型变量 k
k = k + 5;    //先计算表达式 k+5，得到值 10，再把值 10 赋给 k，k 的值为 10', 0, 32, 12),
(19, 22, '赋值表达式示例', 'int k = 5;
System.out.println(k = k + 5);   //输出结果为 10', 0, 32, 13),
(20, 22, '赋值表达式等价语句', 'k = k + 5;
System.out.println(k);', 0, 32, 14),
(21, 22, '链式赋值示例', 'i = j = k = 5;', 0, 32, 15),
(22, 22, '链式赋值等价语句', 'k = 5;
j = k;
i = j;', 0, 32, 16),
(23, 23, 'JShell Tab 补全示例', 'jshell> Math.r
random()   rint(   round(', 0, 35, 17),
(24, 23, 'JShell 方法名自动补全示例', 'jshell> Math.random()', 0, 36, 18),
(25, 25, '二进制整型字面值示例', 'int i1 = 0b1010;
System.out.println(i1);   //输出 10', 0, 38, 19),
(26, 26, '浮点数运算结果的有效位数示例', 'System.out.print(1f/3f);     //结果是 0.33333334，小数点后 8 位，7 位有效数字
System.out.print(1d/3d);     //结果是 0.3333333333333333，小数点后 16 位，16 位有效数字
System.out.print(1000f/3f);  //结果是 333.3334，小数点后 4 位，7 位有效数字
System.out.print(1000d/3d);  //结果是 333.333333333333，小数点后 13 位，16 位有效数字', 0, 40, 20),
(27, 27, '字符变量声明与初始化示例', 'char ch1 = \'中\';       //字符\'中\'赋值给 char 型变量
char ch2 = \'a\';       //字符\'a\'赋值给 char 型变量
char ch3 = \'\\u4E2D\';  //转义序列，用 Unicode 码给 char 型变量赋值
char ch4 = \'\\\"\';      //用双引号“给 char 型变量赋值', 0, 40, 21),
(28, 27, '字符串中未转义双引号的语法错误示例', 'System.out.println("He said,"Hello!");', 0, 40, 22),
(29, 27, '使用转义字符改正后的输出语句', 'System.out.println("He said, \\"Hello!\\"");', 0, 40, 23),
(30, 27, '改正后输出语句的输出结果', 'He said, "Hello!"', 0, 41, 24),
(31, 29, '隐式类型转换示例', 'double d1 = 20;', 0, 42, 25),
(32, 29, '显式类型转换示例', 'int i1 = (int)43.2;', 0, 43, 26),
(33, 29, '四舍五入取整示例', 'int i2 = (int)(43.6+0.5);', 0, 43, 27),
(34, 29, '字符类型与整型、浮点类型相互转换示例', 'long i3 = 98L;
char c1 = (char)i3;    //显式类型转换，c1 的值为''b''
long i4 = ''a'';         //隐式类型转换，i4 的值为 97L
double d2 = ''a'';       //隐式类型转换，d2 的值为 97.0
char c2 = (char)97.7;  //显式类型转换，c2 的值为''a''', 0, 43, 28),
(35, 29, '字符类型与 byte、short、int 相互转换示例', '1:  byte i5 = ''a'';        //隐式类型转换，i5 的值为 97
2:  short i6 = ''\\u0065'';  //隐式类型转换，i6 的值为 101
3:  int i7 = ''a'';         //隐式类型转换，i7 的值为 97
4:  char c1 = 97;         //隐式类型转换，c1 的值为''a''
5:  char c2 = 65536;      //编译器报错，超出范围
6:  byte i8 = ''\\u0165'';   //编译器报错，超出范围
7:  short i9 = ''\\uf365'';  //编译器报错，超出范围
8:  char c3 = i5;         //编译器报错
9:  char c4 = i6;         //编译器报错
10: char c5 = i7;         //编译器报错', 0, 43, 29),
(36, 29, '编译报错语句的显式类型转换改正示例', '5:  char c2 = (char)65536;      //c2 的值为''\\u0000''
6:  byte i8 = (byte)''\\u0165'';   //i8 的值为 101
7:  short i9 = (short)''\\uf365''; //i9 的值为-3227
8:  char c3 = (char)i5;         //c3 的值为''a''
9:  char c4 = (char)i6;         //c4 的值为''a''
10: char c5 = (char)i7;         //c5 的值为''a''', 0, 44, 30),
(37, 29, '布尔类型不能与其他类型相互转换示例', 'boolean b1 = 5>3;         //5>3 的运算结果为 true，布尔变量 b1 的值为 true
float f1 = (int)b1;       //编译器报错
boolean b2 = (boolean)5;  //编译器报错', 0, 44, 31),
(38, 30, '创建 Scanner 对象示例', 'Scanner input = new Scanner(System.in);', 0, 44, 32),
(39, 30, 'nextXXX 方法获取输入示例', 'int a = input.nextInt();        //从键盘读入一个 int 型数值，并将其赋给变量 a
double b = input.nextDouble();  //从键盘读入一个 double 型数值，并将其赋给变量 b', 0, 44, 33),
(40, 30, '程序清单 2-3　ComputeRectArea.java', '1  import java.util.*;
2  public class ComputeRectArea {
3      public static void main(String[] args) {
4          Scanner input = new Scanner(System.in);
5          System.out.print("请输入长方形的宽 cm(如，12.5)：");
6          double width = input.nextDouble();
7          System.out.print("请输入长方形的高 cm(如，8.9)：");
8          double height = input.nextDouble();
9
10         double area = width * height;
11         System.out.printf("长方形面积：%.2f 平方厘米", area);
12     }
13 }', 1, 45, 34),
(41, 30, '程序清单 2-3 运行结果', '请输入长方形的宽 cm(如，12.5)：13.6
请输入长方形的高 cm(如，8.9)：7.8
长方形面积：106.08 平方厘米', 0, 45, 35),
(42, 30, '特定导入示例', 'import java.util.Scanner;', 0, 46, 36),
(43, 30, '通配符导入示例', 'import java.util.*;', 0, 46, 37),
(44, 30, '程序清单 2-4　ComputeAVG.java', '1  import java.util.Scanner;
2  public class ComputeAVG {
3      public static void main(String[] args) {
4          Scanner input = new Scanner(System.in);
5          System.out.print("请输入 3 个数值:");
6          float num1 = input.nextFloat();
7          float num2 = input.nextFloat();
8          float num3 = input.nextFloat();
9
10         //计算平均数
11         float avg = (num1 + num2 + num3) / 3;
12         System.out.printf("平均数是:%.2f", avg);
13     }
14 }', 1, 46, 38),
(45, 30, '程序清单 2-4 运行结果（前两个输入项之后用空格，第三个输入项之后用回车键）', '请输入 3 个数值:3□4□5
平均数是:4.00', 0, 46, 39),
(46, 30, '程序清单 2-4 运行结果（3 个输入项之后都用回车键）', '请输入 3 个数值:3
4
5
平均数是:4.00', 0, 47, 40),
(47, 30, '程序清单 2-4 运行结果（前两个输入项之后用 Tab 键，第三个输入项之后用回车键）', '请输入 3 个数值:3 Tab 4 Tab 5
平均数是:4.00', 0, 47, 41),
(48, 30, '程序清单 2-5　StrCharInputDemo.java', '1  import java.util.*;
2  public class StrCharInputDemo {
3      public static void main(String[] args) {
4          //创建一个输入对象
5          Scanner input = new Scanner(System.in);
6          //next() 方法输入 3 个字符串
7          System.out.print("请输入 3 个空白字符分隔的字符串:");
8          String str1 = input.next();
9          String str2 = input.next();
10         String str3 = input.next();
11         System.out.println("输入的字符串 1 是:" + str1);
12         System.out.println("输入的字符串 2 是:" + str2);
13         System.out.println("输入的字符串 3 是:" + str3);
14         input.nextLine();
15         //nextLine() 方法输入字符串
16         System.out.print("请输入字符串 4:");
17         String str4 = input.nextLine();
18         System.out.println("输入的字符串 4 是:" + str4);
19         //next() 方法输入字符
20         System.out.print("请输入字符串 5:");
21         String str5 = input.nextLine();
22         char ch1 = str5.charAt(0);
23         System.out.println("输入的字符是:" + ch1);
24     }
25 }', 1, 47, 42),
(49, 30, '程序清单 2-5 运行结果', '请输入 3 个空白字符分隔的字符串: Welcome to Java
输入的字符串 1 是: Welcome
输入的字符串 2 是: to
输入的字符串 3 是: Java
请输入字符串 4: Java is fun!
输入的字符串 4 是: Java is fun!
请输入字符串 5: Great
输入的字符是：G', 0, 48, 43),
(50, 31, '未格式化输出的利息计算示例', 'double 存款 = 12464.51;
double 利率 = 0.0125;
double 利息 = 存款 * 利率;
System.out.println("利息是:" + 利息 + "元");', 0, 48, 44),
(51, 31, '未格式化输出的运行结果', '利息是:155.806375 元', 0, 48, 45),
(52, 31, '强制截断小数的利息计算示例', 'double 存款 = 12464.51;
double 利率 = 0.0125;
double 利息 = 存款 * 利率;
System.out.println("利息是:" + (int)(利息 * 100) / 100.0 + "元");', 0, 48, 46),
(53, 31, '强制截断小数的运行结果', '利息是:155.8 元', 0, 48, 47),
(54, 31, 'printf 格式化输出的利息计算示例', 'double 存款 = 12464.51;
double 利率 = 0.0125;
double 利息 = 存款 * 利率;
System.out.printf("利息是%6.2f 元", 利息);', 0, 48, 48),
(55, 31, 'printf 格式化输出的运行结果', '利息是:155.81 元', 0, 49, 49),
(56, 31, '格式字符串示例语句', 'System.out.printf("利息是%6.2f 元", 利息);', 0, 49, 50),
(57, 31, '多格式限定符综合示例', 'String name = "张三";  //定义了一个字符串变量，其值为“张三”
double 存款 = 12464.51;
double 利率 = 0.0125;
int 年数 = 5;
System.out.printf("储户:%s,存款:%e 元,利率:%f,存款年数:%d,利息:%.2f 元", name, 存款, 利率, 年数, (存款 * 利率 * 年数));', 0, 49, 51),
(58, 31, '多格式限定符综合示例运行结果', '储户:张三,存款:1.246451e+04 元, 利率:0.012500, 存款年数:5, 利息:779.03 元', 0, 49, 52),
(59, 31, '域宽使用示例', 'System.out.printf("|%-4d|%3s|%-7.2f|%10.2e|", 12, " Hello", 123.5678, 1234.5678);', 0, 50, 53),
(60, 31, '域宽使用示例运行结果（□ 表示空格字符）', '|12□□Hello|123.57□|□□1.23e+03|', 0, 50, 54),
(61, 31, '前导 0 填充示例', 'System.out.printf("|%04d|%010.2e|", 12, 1234.5678);', 0, 50, 55),
(62, 31, '前导 0 填充示例运行结果', '|0012|001.23e+03|', 0, 50, 56),
(63, 31, '千位分隔符示例', 'System.out.printf("|%,010d|%,015.5f|", 1234567, 1234.5678);', 0, 51, 57),
(64, 31, '千位分隔符示例运行结果', '|01,234,567|00001,234.56780|', 0, 51, 58),
(65, 31, '表格式输出的目标格式', 'x       X^2     X^3
11      121     1331
12      144     1728
13      169     2197', 0, 51, 59),
(66, 31, '程序清单 2-6　FormatOutDemo.java', '1  public class FormatOutDemo {
2      public static void main(String[] args) {
3          System.out.printf("%-8s%-8s%-8s\\n", "X", "X^2", "X^3");
4          System.out.printf("%-8d%-8d%-8d\\n", 11, 11 * 11, 11 * 11 * 11);
5          System.out.printf("%-8d%-8d%-8d\\n", 12, 12 * 12, 12 * 12 * 12);
6          System.out.printf("%-8d%-8d%-8d\\n", 13, 13 * 13, 13 * 13 * 13);
7      }
8  }', 1, 51, 60),
(67, 31, '程序清单 2-6 运行结果', 'x       X^2     X^3
11      121     1331
12      144     1728
13      169     2197', 0, 51, 61),
(68, 33, '后置自增自减示例', 'int i = 2, j = 5;
i++;  //i 变成 3
j--;  //j 变成 4', 0, 53, 62),
(69, 33, '前置自增自减示例', 'int i = 2, j = 5;
++i;  //i 变成 3
--j;  //j 变成 4', 0, 54, 63),
(70, 33, '前置自增参与乘法运算示例', 'int i = 5;
int j = 5 * ++i;', 0, 54, 64),
(71, 33, '后置自增参与乘法运算示例', 'int i = 5;
int j = 5 * i++;', 0, 54, 65),
(72, 33, '同一表达式中对同一变量自增、自减示例', 'int x = 2;
int y = x++ * 2 + ++x;
System.out.println("x:" + x + ";y:" + y);', 0, 54, 66),
(73, 33, '自增自减示例运行结果', 'x:4;y:8', 0, 54, 67),
(74, 33, '程序清单 2-8　TestIncrement.java', '1  public class TestIncrement {
2      public static void main(String[] args) {
3          int x = 1;
4          int y = x++ + ++x * 3 + x++ * 2;
5          System.out.println("x:" + x + ";y:" + y);
6          int i1 = 1;
7          int j1 = ++i1 + i1++ * 3 + i1++ * 2;
8          System.out.println("x:" + i1 + ";y:" + j1);
9      }
10 }', 1, 54, 68),
(75, 33, '程序清单 2-8 运行结果', 'x:4;y:16
x:4;y:14', 0, 55, 69),
(76, 33, '算术混合运算求值过程示例', '5+3*2+(2+4)/3-7%3  //最先计算括号里的
5+3*2+6/3-7%3      //从左向右，计算乘法
5+6+6/3-7%3        //计算除法
5+6+2-7%3          //计算取余
5+6+2-1            //从左向右，计算第一个加法
11+2-1             //计算第二个加法
13-1               //计算减法
12', 0, 55, 70),
(77, 33, '程序清单 2-9　摄氏转华氏.java', '1  public class 摄氏转华氏 {
2      public static void main(String[] args) {
3          Scanner input = new Scanner(System.in);
4

5          System.out.print("请输入一个摄氏温度:");
6          double 摄氏温度 = input.nextDouble();
7

8          //摄氏温度转换为华氏温度
9          double 华氏温度 = 32 + 9.0 / 5 * 摄氏温度;
10         System.out.println("摄氏温度:" + 摄氏温度
11                 + "对应的华氏温度是:" + 华氏温度);
12     }
13 }', 1, 56, 71),
(78, 33, '程序清单 2-9 运行结果', '请输入一个摄氏温度：36.5
摄氏温度：36.5对应的华氏温度是：97.7', 0, 56, 72),
(79, 34, '增强赋值运算符自动类型转换错误示例', 'int total = 0;
total = total + 5.5;  //这一行语句是错误的，不能将 double 类型隐式转换为 int 类型', 0, 57, 73),
(80, 34, '增强赋值运算符自动类型转换正确示例', 'int total = 0;
total += 5.5;  //这一行语句等价于 total = (int)(total + 5.5);', 0, 57, 74),
(81, 37, '条件运算符求最大值示例', 'int max = x > y ? x : y;', 0, 59, 75),
(82, 38, '运算符优先级示例语句', 'boolean b = 5+3*4 < 3*(2+3)-4 || 3*(7-4)+1 < 8;', 0, 59, 76),
(83, 38, '运算符优先级求值过程示例', 'boolean b = 5+3*4 < 3*(2+3)-4 || 3*(7-4)+1 < 8;   //计算(2+3)
boolean b = 5+3*4 < 3*5-4 || 3*(7-4)+1 < 8;       //计算(7-4)
boolean b = 5+3*4 < 3*5-4 || 3*3+1 < 8;           //计算3*4
boolean b = 5+12 < 3*5-4 || 3*3+1 < 8;            //计算3*5
boolean b = 5+12 < 15-4 || 3*3+1 < 8;             //计算3*3
boolean b = 5+12 < 15-4 || 9+1 < 8;               //计算5+12
boolean b = 17 < 15-4 || 9+1 < 8;                 //计算15-4
boolean b = 17 < 11 || 9+1 < 8;                   //计算17<11
boolean b = false || 9+1 < 8;                     //计算9+1
boolean b = false || 10 < 8;                      //计算10<8
boolean b = false || false;                       //计算false||false
boolean b = false;', 0, 59, 77),
(84, 40, '程序清单 2-10　TestTriMathMethods.java', '1  public class TestTriMathMethods {
2      public static void main(String[] args) {
3          System.out.println("Math.sin(Math.PI/2)=" + Math.sin(Math.PI/2));
4          System.out.println("Math.sin(Math.PI/6)=" + Math.sin(Math.PI/6));
5          System.out.println("Math.cos(Math.PI/2)=" + Math.cos(Math.PI/2));
6          System.out.println("Math.cos(Math.PI/6)=" + Math.cos(Math.PI/6));
7          System.out.println("Math.cos(0)=" + Math.cos(0));
8          System.out.println("Math.asin(0.5)=" + Math.asin(0.5));
9          System.out.println("Math.acos(0.5)=" + Math.acos(0.5));
10         System.out.println("Math.acos(-1)=" + Math.acos(-1));
11         System.out.println("Math.atan(1.0)=" + Math.atan(1.0));
12         System.out.println("Math.toRadians(90)=" + Math.toRadians(90));
13         System.out.println("Math.toDegrees(Math.PI/6)=" + Math.toDegrees(Math.PI/6));
14     }
15 }', 1, 61, 78),
(85, 40, '程序清单 2-10 运行结果', 'Math.sin(Math.PI/2)=1.0
Math.sin(Math.PI/6)=0.49999999999999994
Math.cos(Math.PI/2)=6.123233995736766E-17
Math.cos(Math.PI/6)=0.8660254037844387
Math.cos(0)=1.0
Math.asin(0.5)=0.5235987755982989
Math.acos(0.5)=1.0471975511965979
Math.acos(-1)=3.141592653589793
Math.atan(1.0)=0.7853981633974483
Math.toRadians(90)=1.5707963267948966
Math.toDegrees(Math.PI/6)=29.999999999999996', 0, 61, 79),
(86, 41, '程序清单 2-11　TestExpMathMethod.java', '1  public class TestExpMathMethod {
2      public static void main(String[] args) {
3          System.out.println("Math.exp(2.5)=" + Math.exp(2.5));
4          System.out.println("Math.exp(3)=" + Math.exp(3));
5          System.out.println("Math.log(5.5)=" + Math.log(5.5));
6          System.out.println("Math.log10(5.5)=" + Math.log10(5.5));
7          System.out.println("Math.pow(3,2)=" + Math.pow(3,2));
8          System.out.println("Math.pow(2,3)=" + Math.pow(2,3));
9          System.out.println("Math.pow(2.5,3.5)=" + Math.pow(2.5,3.5));
10         System.out.println("Math.sqrt(4)=" + Math.sqrt(4));
11         System.out.println("Math.sqrt(4.5)=" + Math.sqrt(4.5));
12     }
13 }', 1, 62, 80),
(87, 41, '程序清单 2-11 运行结果', 'Math.exp(2.5)=12.182493960703473
Math.exp(3)=20.085536923187668
Math.log(5.5)=1.7047480922384253
Math.log10(5.5)=0.7403626894942439
Math.pow(3,2)=9.0
Math.pow(2,3)=8.0
Math.pow(2.5,3.5)=24.705294220065465
Math.sqrt(4)=2.0
Math.sqrt(4.5)=2.1213203435596424', 0, 62, 81),
(88, 42, '程序清单 2-12　TestRoundMethods.java', '1  public class TestRoundMethods {
2      public static void main(String[] args) {
3          System.out.print("Math.ceil(2.8)=" + Math.ceil(2.8));
4          System.out.println("\\tMath.ceil(-2.8)=" + Math.ceil(-2.8));
5          System.out.print("Math.ceil(2.1)=" + Math.ceil(2.1));
6          System.out.println("\\tMath.ceil(-2.1)=" + Math.ceil(-2.1));
7          System.out.print("Math.floor(2.8)=" + Math.floor(2.8));
8          System.out.println("\\tMath.floor(-2.8)=" + Math.floor(-2.8));
9          System.out.print("Math.floor(2.1)=" + Math.floor(2.1));
10         System.out.println("\\tMath.floor(-2.1)=" + Math.floor(-2.1));
11         System.out.print("Math.rint(2.1)=" + Math.rint(2.1));
12         System.out.println("\\tMath.rint(2.5)=" + Math.rint(2.5));
13         System.out.print("Math.rint(2.8)=" + Math.rint(2.8));
14         System.out.println("\\tMath.rint(2.0)=" + Math.rint(2.0));
15         System.out.print("Math.rint(-2.1)=" + Math.rint(-2.1));
16         System.out.println("\\tMath.rint(-2.5)=" + Math.rint(-2.5));
17         System.out.print("Math.round(2.6f)=" + Math.round(2.6f));   //返回 int
18         System.out.println("\\tMath.round(2.4f)=" + Math.round(2.4f));   //返回 int
19         System.out.print("Math.round(2.6)=" + Math.round(2.6));   //返回 long
20         System.out.println("\\tMath.round(2.4)=" + Math.round(2.4));   //返回 long
21     }
22 }', 1, 63, 82),
(89, 42, '程序清单 2-12 运行结果', 'Math.ceil(2.8)=3.0	Math.ceil(-2.8)=-2.0
Math.ceil(2.1)=3.0	Math.ceil(-2.1)=-2.0
Math.floor(2.8)=2.0	Math.floor(-2.8)=-3.0
Math.floor(2.1)=2.0	Math.floor(-2.1)=-3.0
Math.rint(2.1)=2.0	Math.rint(2.5)=2.0
Math.rint(2.8)=3.0	Math.rint(2.0)=2.0
Math.rint(-2.1)=-2.0	Math.rint(-2.5)=-2.0
Math.round(2.6f)=3	Math.round(2.4f)=2
Math.round(2.6)=3	Math.round(2.4)=2', 0, 63, 83),
(90, 44, 'ComputeTriangleArea 程序框架（类声明）', 'public class ComputeTriangleArea{
    //具体细节待补充
}', 0, 65, 84),
(91, 44, 'ComputeTriangleArea 程序框架（main 方法）', 'public class ComputeTriangleArea{
    public static void main(String[] args){
        //step 1：输入一个三角形的三条边
        //step 2：计算三角形周长的一半
        //step 3：计算三角形的面积
        //step 4：输出三角形的面积
    }
}', 0, 65, 85),
(92, 44, '三角形周长一半计算语句', 'p = (a+b+c)/2;', 0, 66, 86),
(93, 44, '三角形面积计算语句', 'area = Math.sqrt(p*(p-a)*(p-b)*(p-c));', 0, 66, 87),
(94, 44, '三角形面积输出语句', 'System.out.printf("三角形的面积是:%.2f",area);', 0, 66, 88),
(95, 44, '程序清单 2-13　ComputeTriangleArea.java', '1  public class ComputeTriangleArea {
2      public static void main(String[] args) {
3          //step 1:输入一个三角形的3条边
4          Scanner input = new Scanner(System.in);
5

6          System.out.print("请输入三角形的3条边:");
7          double a = input.nextDouble();
8          double b = input.nextDouble();
9          double c = input.nextDouble();
10

11         //step 2：计算三角形周长的一半
12         double p = (a+b+c)/2;
13

14         //step 3:计算三角形的面积
15         double area = Math.sqrt(p*(p-a)*(p-b)*(p-c));
16

17         //step 4:输出三角形的面积
18         System.out.printf("三角形的面积是:%.2f",area);
19     }
20 }', 1, 66, 89),
(96, 44, '程序清单 2-13 运行结果', '请输入三角形的3条边：3 4 5
三角形的面积是：6.00', 0, 66, 90),
(97, 48, 'javadoc 命令执行', 'E:\\temp> javac TestJavaDoc.java
E:\\temp> javadoc -d e:\\temp\\doc TestJavaDoc.java
正在加载源文件 TestJavaDoc.java...
正在构造 Javadoc 信息...
正在构建所有程序包和类的索引...
标准 Doclet 版本 17.0.4+11-LTS-179
正在构建所有程序包和类的树...
正在生成 e:\\temp\\doc\\TestJavaDoc.html...
正在生成 e:\\temp\\doc\\package-summary.html...
正在生成 e:\\temp\\doc\\package-tree.html...
正在生成 e:\\temp\\doc\\overview-tree.html...
正在构建所有类的索引...
正在生成 e:\\temp\\doc\\allclasses-index.html...
正在生成 e:\\temp\\doc\\allpackages-index.html...
正在生成 e:\\temp\\doc\\index.html...
正在生成 e:\\temp\\doc\\index-all.html...
正在生成 e:\\temp\\doc\\help-doc.html...', 0, 69, 91),
(98, 48, 'Main1.java', 'package test1;
/**
 * 这是一个测试类，用于说明 javadoc
 * @author Liu
 * @version 1.0
 */
public class Main1 {
    /**
     * DISC:商品固定折扣
     */
    public static final double DISC = 0.9;
    /**
     * 该方法计算商品1和商品2的总售价
     * @param p1 商品1的价格
     * @param p2 商品2的价格
     * @return 商品1和商品2 的总售价
     */
    public static double add(double p1, double p2) {
        return p1+p2;
    }
    /**
     * 这是主方法
     * @param args 主方法的参数
     */
    public static void main(String[] args) {
        System.out.println("商品1和商品2的售价:" + add(12.5,23.7));
    }
}', 0, 71, 92),
(99, 48, 'Eclipse 生成文档时的控制台信息', '正在加载程序包 test1 的源文件...
正在构造 Javadoc 信息...
正在构建所有程序包和类的索引...
标准 Doclet 版本 17.0.4+11-LTS-179
正在构建所有程序包和类的树...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\test1\\Main1.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\test1\\package-summary.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\test1\\package-tree.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\module-summary.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\test1\\class-use\\Main1.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\TestJavaDoc\\test1\\package-use.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\overview-tree.html...
正在构建所有类的索引...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\allclasses-index.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\allpackages-index.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\index-files\\index-1.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\index-files\\index-2.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\index-files\\index-3.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\index.html...
正在生成 D:\\javaNewCode\\src\\TestJavaDoc\\doc\\help-doc.html...', 0, 71, 93),
(100, 51, '程序清单 2-15　ShowCompileError.java', '1  package chapt2;
2  public class ShowCompileError {
3      public static void main(String[] args) {
4          int i;
5          System.out.println("i="+i);
6      }
7 }', 1, 74, 94),
(101, 52, '程序清单 2-16　ShowRuntimeError.java', '1  package chapt2;
2  import java.util.Scanner;
3  public class ShowRuntimeError {
4      public static void main(String[] args) {
5          Scanner input = new Scanner(System.in);
6
7          System.out.print("请输入被除数:");
8          int 被除数 = input.nextInt();
9          System.out.print("请输入除数:");
10         int 除数 = input.nextInt();
11
12         int 商 = 被除数 / 除数;
13         System.out.println("被除数: " + 被除数 +
14                 ", 除数: " + 除数 + ", 商: " + 商);
15     }
16 }', 1, 75, 95),
(102, 52, '程序清单 2-16 运行结果（除数不为零）', '请输入被除数：23
请输入除数：5
被除数: 23, 除数: 5, 商: 4', 0, 75, 96),
(103, 52, '程序清单 2-16 运行结果（除数为零）', '请输入被除数：23
请输入除数：0
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at chapt2.ShowRuntimeError.main(ShowRuntimeError.java:12)', 0, 75, 97),
(104, 53, '程序清单 2-17　ShowLogicError.java', '1  public class ShowLogicError {
2      public static void main(String[] args) {
3          Scanner input = new Scanner(System.in);
4
5          System.out.print("请输入一个摄氏温度:");
6          double 摄氏温度 = input.nextDouble();
7
8          //摄氏温度转换为华氏温度
9          double 华氏温度 = 32 + (9 / 5) * 摄氏温度;
10         System.out.println("摄氏温度: " + 摄氏温度
11                 + " 对应的华氏温度是: " + 华氏温度);
12     }
13 }', 1, 76, 98),
(105, 53, '程序清单 2-17 运行结果', '请输入一个摄氏温度：36.5
摄氏温度：36.5对应的华氏温度是：68.5', 0, 76, 99);

INSERT INTO `knowledge_content` (`id`, `chapter_id`, `knowledge_point_id`, `content_type`, `content`, `page`, `page_order`) VALUES
(206, 23, 20, 1, '首先，从 Windows 进入 JShell，有两种方式。第一种方式是使用命令提示符，单击 Windows 系统的“开始”菜单按钮，找到菜单项“Windows 系统”，在菜单项“Windows 系统”下找到“命令行提示符”，如图 2-1(a) 所示。然后单击之，系统会弹出命令行窗口，在命令行窗口中输入 JShell，启动 JShell，如图 2-1(b) 所示。第二种方式是使用 Windows PowerShell，单击 Windows 系统的“开始”菜单按钮，找到菜单项“Windows PowerShell”，在菜单项“Windows PowerShell”下找到“Windows PowerShell”，如图 2-2(a) 所示。然后单击之，系统会弹出“Windows PowerShell”窗口，在“Windows PowerShell”窗口中输入 JShell，启动 JShell，如图 2-2(b) 所示。（图 2-1　使用命令提示符启动 JShell：从“Windows 系统”菜单打开“命令行提示符”，输入 JShell 启动）（图 2-2　使用 Windows PowerShell 启动 JShell：从“Windows PowerShell”菜单打开 PowerShell 窗口，输入 JShell 启动）', 33, 1),
(207, 23, 20, 1, '在 JShell 启动后，可以在提示符后输入 Java 语句，如 char ch1=\'A\'，如图 2-3(a) 所示。输出结果解释了该语句的动作。再如，输出变量 ch1 的语句 System.out.println(ch1)，如图 2-3(b) 所示。在 JShell 中，表达式后是否加分号（；），执行结果一样，如图 2-3(c) 所示。（图 2-3　在 JShell 中执行语句：输入 char ch1=\'A\' 与 System.out.println(ch1) 并查看输出，表达式后加或不加分号执行结果一样）', 33, 2),
(208, 23, 20, 1, 'JShell 可以对表达式立即进行计算。例如，输入一个表达式 1+3*4，如图 2-4(a) 所示。$3 是系统为该表达式自动生成的一个变量，存储该表达式的值。又如，定义一个整型变量 int x=2，再计算 2*x+5，如图 2-4(b) 所示，$5 也是系统为表达式自动生成的一个变量，存储该表达式的值。另外，还可以用命令 /vars 列出所有已声明或生成的变量，如图 2-4(c) 所示。与命令 /vars 类似的有 /methods（列出所有已声明的方法及其签名）、/types（列出所有声明的类型）、/imports（列出所有当前声明的导入）。（图 2-4　JShell 对表达式立即计算：表达式 1+3*4 与 2*x+5 的计算结果分别存入系统自动生成的变量 $3、$5，命令 /vars 可列出所有已声明或生成的变量）', 33, 3),
(209, 23, 20, 1, 'JShell 可以使用命令 /edit 打开一个编辑器，如果命令 /edit 后面没有跟任何选项，则以当前活动的代码片段作为编辑器的默认内容，如图 2-5 所示。（图 2-5　命令 /edit：打开编辑器，默认内容为当前活动的代码片段）', 34, 1),
(210, 23, 20, 1, '用户可以在编辑器中对代码进行编辑，输入新的代码，如图 2-6(a) 所示。当编辑完毕后，可以单击“Accept”按钮，这时，在命令行窗口可以得到运算结果（y=10，z=20），如图 2-6(b) 所示。最后，单击“Exit”按钮可以退出编辑器，返回命令行窗口。（图 2-6　在 /edit 编辑器中编辑代码：单击“Accept”得到运算结果 y=10、z=20，单击“Exit”退出编辑器）', 34, 2),
(211, 23, 20, 1, '可以使用命令 /save，保存当前所有输入的代码段，如图 2-7(a) 所示。被保存文件的文件名是 test，无后缀名，该文件存储在当前目录中。在该示例中，当前目录为 C:\\Users\\lenovo。打开 test 文件，其内容如图 2-7(b) 所示，正好是当前输入的所有代码段。（图 2-7　命令 /save 存储当前代码片段：保存后 test 文件的内容为当前输入的全部代码段，即 char ch1=\'A\';、System.out.print(ch1)、1+3*4、int x=2;、2*x+5、int y=10;、int z=2*y;）', 34, 3),
(212, 23, 20, 1, '命令 /history 用来显示历史命令记录，如图 2-8(a) 所示。命令 /list 用来输出所有先前键入的代码段以及这些代码段所对应的唯一标识 ID，如图 2-8(b) 所示。第一行代码段为 char ch1=\'A\'，其对应的代码段 ID 是 1。第二行代码段为 System.out.println(ch1)，其对应的代码段 ID 是 2。默认情况下，命令 /list 输出不包含任何导致错误的代码段，仅仅显示有效的表达式或语句。如果要查看所有以前键入的代码（包括错误），可以将 -all 参数传递给命令 /list，即输入“/list -all”。此时，输出将包含任何启动代码以及任何有效或无效的代码段。（图 2-8　命令 /history 与 /list：/history 显示历史命令记录，/list 输出代码段及其唯一标识 ID）', 34, 4),
(213, 23, 20, 1, '命令 /save 还可以存储指定的代码片段，使用代码段 ID 来指定。一个示例如图 2-9 所示，把代码段 ID 为 1、2、3、4 的表达式或语句存储到文件 test2.txt 中。其中，1-3 表示代码段 ID 为 1、2、3 的 3 行表达式或语句，空格之后，接一个代码段 ID 为 4 的表达式或语句，test2.txt 是存储文件的文件名。在命令 /save 后，指定一个范围内的 ID 时，指明起始 ID 号和终止 ID 号，中间用短横线相连；指定离散的多个 ID 时，多个 ID 之间用空格分隔。（图 2-9　命令 /save 存储指定的代码片段：把代码段 ID 为 1-3 及 4 的表达式或语句存储到文件 test2.txt 中）', 35, 1),
(214, 23, 20, 1, '命令 /drop 用于删除先前的代码片段。例如，删除代码段 ID 为 3 的代码片段，如图 2-10 所示。系统生成的变量 $3 被删除，再次使用命令 /list 时，可以看到代码段 ID 为 3 的代码片段没有了。（图 2-10　命令 /drop 删除代码片段：删除代码段 ID 为 3 的代码片段后，/list 中不再显示该代码段）', 35, 2),
(215, 23, 20, 1, '命令 /open 用于打开任何先前保存的文件重新计算，例如，先前保存了一个 test 文件，使用命令 /open 打开它，如图 2-11 所示。（图 2-11　命令 /open：在 JShell 中输入 /open test，重新计算先前保存的 test 文件中的代码段）', 35, 3),
(216, 23, 20, 1, 'JShell 还有一个有用的特性是“Tab 补全”。例如，用户如果输入 Math. 再按一次 Tab 键，那么就会得到 Math（数学类）能访问常量和能调用方法的一个列表，如图 2-12 所示。（图 2-12　Math. 的 Tab 补全：列表包含常量 E、PI 及 abs、absExact、acos、addExact、asin、atan、atan2、cbrt、ceil、cos、cosh、decrementExact、exp、expm1、floor、floorDiv、floorMod、fma、getExponent、hypot、incrementExact、log、log10、log1p、max、min、multiplyExact、multiplyFull、multiplyHigh、negateExact、nextAfter、nextDown、nextUp、pow、random、rint、round、scalb、signum、sin、sinh、sqrt、subtractExact、tan、tanh、toDegrees、toIntExact、toRadians、ulp 等常量和方法）', 35, 4),
(217, 23, 20, 1, '如果继续在 Math. 之后输入字母 r，再按一次 Tab 键，就会得到一个更小的列表：', 35, 5),
(218, 23, 20, 1, '如果在 Math. 之后继续输入 ran，再按一次 Tab 键，那么方法名会自动补全为 random()。', 36, 1),
(219, 23, 20, 1, '另外，如果希望重复运行一个命令，可以通过连续按 ↑ 键，直到看到想要运行或编辑的命令行。在向上寻找的过程中，也可以连续按 ↓ 键，向下搜寻所需要的命令行。在找到所需的命令行后，可以按回车键直接执行，也可以进行编辑。在编辑时，可通过使用 ← 和 → 键移动命令行中的光标位置，增加或删除字符。编辑完命令后，可以按回车键执行。', 36, 2),
(220, 23, 20, 1, '命令 /exit 可退出 JShell。', 36, 3),
(221, 23, 20, 1, '更多关于 JShell 的信息，可参考网址 https://dev.java/learn/jshell-tool/#anchor6。', 36, 4),
(222, 24, NULL, 1, 'Java 语言的数据类型分为基本数据类型和引用数据类型两大类，如图 2-13 所示。基本数据类型又可分为数值类型和布尔类型。数值类型又可分为整数类型和浮点类型。整数类型包括 4 种有符号整数类型（byte、short、int、long）和一种无符号整数类型（char，又称字符类型）。浮点类型又可分为单精度浮点型（float）和双精度浮点型（double）。因此，Java 语言的基本数据类型有 7 种，每种基本数据类型数据的取值范围和占用的存储空间都是固定的，不依赖于具体的计算机。（图 2-13　Java 数据类型的分类：基本数据类型分为数值类型和布尔类型，数值类型分为整数类型和浮点类型，引用数据类型另行详述）', 36, 5),
(223, 24, NULL, 1, '每种基本数据类型有其对应的字面值（Literal），字面值是直接出现在程序中的常量值。', 36, 6),
(224, 24, NULL, 1, '在编程时，经常会用到字面值，如 34、\'A\'、3.14、true、false 等。在下面各小节分别叙述各基本类型及其对应的字面值。', 37, 1),
(225, 25, 21, 1, 'Java 语言提供了 5 种整数类型，其表示值的范围和占用存储空间大小如表 2-3 所示。其中，byte 称为字节类型，表示 1 个字节长度的有符号整数（有符号整数是指具有正负值的整数。在计算机中，整数以补码形式存储，存储单元的最高比特位表示符号位。例如，short 占 2 个字节的存储单元，最高比特位是表示正负的符号位，只有 15 个比特位表示整数的值）。short 称为短整型，表示 2 个字节长度的有符号整数。char 称为字符型，表示 2 个字节长度的无符号整数（无符号整数，是指没有负值的整数。在计算机中，用补码表示整数时，无须用存储单元的最高比特位表示正负号。例如，char 占 2 个字节的存储单元，16 个比特位全部用于表示整数值），对应字符的 Unicode 编码。int 称为整型，表示 4 个字节长度的整数。long 称为长整型，表示 8 个字节长度的有符号整数。', 37, 2),
(226, 25, 21, 1, '表 2-3　整数类型\n| 类型 | 表示值的范围 | 存储空间/B |\n| --- | --- | --- |\n| byte | -2⁷~2⁷-1（-128~127） | 1 |\n| short | -2¹⁵~2¹⁵-1（-32768~32767） | 2 |\n| char | 0~2¹⁶-1（0~65535） | 2 |\n| int | -2³¹~2³¹-1（-2147483648~2147483647） | 4 |\n| long | -2⁶³~2⁶³-1（-9223372036854775808~9223372036854775807） | 8 |', 37, 3),
(227, 25, 21, 1, '程序员应根据需要选择最合适的数据类型，节约存储空间。例如，已知一个变量存储的整数在一个字节范围内，就应该定义该变量的数据类型为 byte 类型。为了简单性和一致性，本书在默认情况下，均使用 int 类型表示整数。', 37, 4),
(228, 25, 21, 1, '整型字面值是可以赋值给一个整数类型的常量值，如 123、24 等。整型字面值默认为 int 型，它的值为 -2³¹（-2147483648）~2³¹-1（2147483647）。为了表示一个长整型的整型字面值，需要在整型字面值后加 L 或 l。例如，为了在程序中使用一个整型字面量 2147483650，由于该整型字面值超出了 int 型值的范围，所以必须在其后加上 L 或 l（ell），即 2147483650L 或 2147483650l。', 37, 5),
(229, 25, 21, 1, '为了提高程序易读性，推荐使用 L，因为小写字母 l 和数字 1 容易混淆。', 37, 6),
(230, 25, 21, 1, '整型字面值是可以赋值给一个整型变量的常量值，要求整型字面值不能超过整型变量的取值范围。例如，byte b1 = 125 是合法的，而 byte b2 = 128 就是不正确的，会导致编译错误。因为 byte 类型变量表示的最大值是 127，而 128 已经超出其范围了。', 37, 7),
(231, 25, 21, 1, '默认情况下，整型字面值是一个十进制整数。Java 语言也支持二进制、八进制、十六进制的整型字面值。', 37, 8),
(232, 25, 21, 1, '二进制整型字面值使用前置 0b（零 b）或 0B，如二进制数 0b1010 表示十进制整数 10。', 37, 9),
(233, 25, 21, 1, '八进制整型字面值使用前置 0（零），如八进制数 017 表示十进制整数 15。', 37, 10),
(234, 25, 21, 1, '十六进制整型字面值使用前置 0x（零 x）或 0X，如十六进制数 0xA5 表示十进制整数 165。', 38, 1),
(235, 25, 21, 1, '在使用时，凡是十进制整型字面值使用的地方，二进制、八进制、十六进制整型字面值都可以使用。例如，下面一段代码：', 38, 2),
(236, 25, 21, 1, '在 JShell 中验证其输出，如图 2-14 所示。（图 2-14　验证二进制整型字面值的使用：在 JShell 中执行 int i1=0b1010; 和 System.out.println(i1)，输出 10）', 38, 3),
(237, 25, 21, 1, '一些数值型字面值数字比较多，为了提高程序的易读性，Java 语言允许在一个数值型（整数类型、浮点类型）字面值中使用下画线分隔两个数字，但是下画线不能位于开头和结尾处。例如，下面的整型字面值是正确的：', 38, 4),
(238, 25, 21, 1, '123_456_789、10_5678_3456、9876_5432_1234L、017_7777_7777_7777_7777_7777L、0b1011_1111_1111_1100_1100_1010_1111L、0xdfff_ffff_ffff_efffL、0X00_FF_FF（下画线连用）', 38, 5),
(239, 25, 21, 1, '但是，_123 或 123_ 是不正确的，因为下画线不能位于开头和结尾处，只能位于两个数字之间。', 38, 6),
(240, 25, 21, 1, '另外，char 是字符型，本质上是一个无符号整数类型，由于字符字面值有一定区别，所以将在 2.5.3 节中单独叙述。', 38, 7),
(241, 26, 22, 1, 'Java 语言提供两种浮点类型：float（单精度浮点型）和 double（双精度浮点型），其范围和存储空间如表 2-4 所示。', 38, 8),
(242, 26, 22, 1, '表 2-4　浮点型数据类型\n| 类型 | 范围 | 存储空间/B |\n| --- | --- | --- |\n| float | 正数范围：1.4E-45~3.4028235E+38；负数范围：-3.4028235E+38~-1.4E-45 | 4 |\n| double | 正数范围：4.9E-324~1.7976931348623157E+308；负数范围：-1.7976931348623157E+308~4.9E-324 | 8 |', 38, 9),
(243, 26, 22, 1, '注意：float、double 都可用来表示带小数点的实数，在编程语言中被称为浮点数。IEEE 754 是用于在计算机上表示浮点数的标准。Java 语言对 float 类型采用 32 位的 IEEE 754，对 double 类型采用 64 位的 IEEE 754。自 Java SE 15 开始，使用 2019 版 IEEE 754 标准。在 Java SE 15 之前，Java 语言使用的是 1985 版 IEEE 754 标准。', 38, 10),
(244, 26, 22, 1, '浮点型字面值是带小数点的，默认为双精度浮点型 double。例如，3.14 是一个浮点型字面值，被 Java 编译器理解为 double 型而不是 float 型。为了表示单精度浮点型字面值，需要在浮点型字面值后加字母 f 或 F。例如，3.14f 或 3.14F 均被编译器理解为 float 型。虽然浮点型字面值默认是 double 型，但是也可以在浮点型字面值后面加上字母 d 或 D 明确加以表示，例如，3.14d 或 3.14D 均被 Java 编译器理解为 double 型。', 38, 11),
(245, 26, 22, 1, '如果一个非零的浮点型字面值太大或太小，超过 double 型的取值范围（即溢出），则 Java 编译器会报错。', 38, 12),
(246, 26, 22, 1, 'Java 语言用于表示溢出和出错情况的 3 个特殊的浮点数值是正无穷大（Infinity）、负无穷大（-Infinity）和 NaN（Not a Number，不是一个数字）。通过使用常量表达式（如 1f/0f、-1d/0d、0.0/0），可以表示无限（Infinity）、NaN，而不会产生错误，如图 2-15 所示。（图 2-15　用常量表达式表示 Infinity 和 NaN：在 JShell 中执行 1f/0f、-1d/0d、0.0/0 等表达式）', 38, 13),
(247, 26, 22, 1, '浮点型字面值也可以用 a×10ᵇ 形式的科学记数法表示。例如，1357.68 的科学记数法形式是 1.35768×10³，0.00002158 的科学记数法形式是 2.158×10⁻⁵。', 39, 1),
(248, 26, 22, 1, 'Java 语言在使用科学记数法表示浮点数时，去掉了乘号“×”，10 的整数次幂由字母 e 或 E 后接一个整数（可带正负号）表示。例如，1357.68 可以表示成 1.35768E3 或 1.35768E+3，0.00002158 可以表示成 2.158e-5。字母 E 大小写均可以。', 39, 2),
(249, 26, 22, 1, '在浮点型字面值中，下画线也可用作数字之间的分隔符，既可用于表示整数部分的数字之间，也可用于表示小数的数字之间，还可用于表示指数部分的数字。但是，小数点前后不允许有下画线。', 39, 3),
(250, 26, 22, 1, '下面是 float 型字面值的合法例子：', 39, 4),
(251, 26, 22, 1, '5.f、.5f、1f、3.14f、1e-1f、.1E2f、12_34.5_678_923f、5.003_7654e+2_3f、2e-3f', 39, 5),
(252, 26, 22, 1, '下面是 double 型字面值的合法例子：', 39, 6),
(253, 26, 22, 1, '5.d、.5d、0d、3.14d、1e-1d、.1E2d、12_34.5_678_923d、5.003_7654e+2_03d、2e-2_01d', 39, 7),
(254, 26, 22, 1, 'double 类型比 float 类型的精度要高，double 类型值具有 16 个有效数字，第 17 位是估算值，float 类型值具有 7 个有效数字，第 8 位是估算值。例如，下面在 JShell 中对 double、float 的有效数字进行测试，如图 2-16 和图 2-17 所示。（图 2-16　测试 double 类型的有效数字：变量 d1、d2、d3、d4 在 JShell 中的输出）（图 2-17　测试 float 类型的有效数字：变量 f1、f2、f3、f4 在 JShell 中的输出）', 39, 8),
(255, 26, 22, 1, '变量 d1、d2 小数点后分别是 17 位、16 位数字。变量 d3、d4 小数点前都有 4 位数字，小数点后都是 13 位。于是，变量 d3、d4 的数字总位数都是 17 位。因此，double 变量有效数字是 16 位，第 17 位是估算值。', 39, 9),
(256, 26, 22, 1, '变量 f1、f2 小数点后分别是 7 位、8 位数字。变量 f3、f4 小数点前都有 4 位数字，小数点后都是 4 位。于是，变量 f3、f4 的数字总位数都是 8 位。因此，float 类型变量的有效数字是 7 位，第 8 位是估算值。', 39, 10),
(257, 26, 22, 1, '下面的代码展示了浮点数运算结果的有效位数：', 40, 1),
(258, 26, 22, 1, '在 JShell 中验证，如图 2-18 所示。（图 2-18　在 JShell 中验证浮点数运算结果的有效位数）', 40, 2),
(259, 26, 22, 1, '另外，需要注意的是，Java 语言的 double 类型在计算时存在精度损失问题，例如，0.1+0.2 的数学运算结果是 0.3，而实际运算时，结果不是 0.3，如图 2-19 所示。（图 2-19　double 类型计算的精度损失：0.1+0.2 的实际结果为 0.30000000000000004）', 40, 3),
(260, 26, 22, 1, '这是因为输入的十进制 double 类型的数值在进行计算的时候，计算机会先将其转换为二进制数据，然后再进行相关的运算。然而，在十进制数转换为二进制数的过程中，有些十进制数是无法使用一个有限的二进制数来表达的，换言之，就是在转换的时候出现了精度损失问题。因此，上面的情况就出现了 0.1+0.2 的结果是 0.30000000000000004，而不是 0.3。', 40, 4),
(261, 27, 23, 1, '字符数据类型 char 表示单个字符，占用两个字节，本质上是一个无符号整数，表示 0~65535 的整数，对应字符的 Unicode 编码为 \\u0000~\\uffff。字符字面值是用单引号括住的单个字符或是一个转义序列（Escape Sequence）。例如，下面一段代码：', 40, 5),
(262, 27, 23, 1, '注意：用双引号括起来的单个字符，表示的是字符串字面值，而单引号括起来的单个字符表示的是字符字面值。', 40, 6),
(263, 27, 23, 1, '一些特殊字符，如单引号、双引号、反斜杠等在 Java 语言中具有特殊含义。例如，当 Java 编译器扫描到第一个双引号时，它认为这是一个字符串字面值的开始标志；当它继续扫描到第二个双引号时，它认为这是一个字符串字面值的结束标志。如果 Java 编译器没有扫描到第二个双引号，就会报错。在开始标志和结束标志之间的字符序列，就是字符串的内容。例如：输出语句', 40, 7),
(264, 27, 23, 1, '会产生语法错误。如果需要在程序中输出双引号，需要使用转义字符（Escape Character）反斜杠 \\，改正之后的输出语句是：', 40, 8),
(265, 27, 23, 1, '输出的字符串是：', 41, 1),
(266, 27, 23, 1, '通过使用转义字符反斜杠，把双引号的特殊含义去掉了，转变成了普通的双引号，这就是转义的由来。转义序列由反斜杠（\\）后面加上一个字符或一些数位组成。例如，\\u4E2D 是表示一个 Unicode 的转义字符，代表字符\'中\'，而 \\" 表示单个字符双引号。转义序列中的符号是被整体解释的，一个转义序列被作为单个字符处理。一些特殊字符的转义序列如表 2-5 所示。', 41, 2),
(267, 27, 23, 1, '表 2-5　转义序列\n| 转义序列 | 名称 | Unicode 码 |\n| --- | --- | --- |\n| \\b | 退格键（Backspace，BS） | \\u0008 |\n| \\s | 空格键（Space，SP） | \\u0020 |\n| \\t | 水平制表符（Horizontal Tab，HT） | \\u0009 |\n| \\n | 换行符（Line Feed，LF） | \\u000a |\n| \\f | 换页符（Form Feed，FF） | \\u000c |\n| \\r | 回车符（Carriage Return，CR） | \\u000d |\n| \\" | 双引号（Double Quote） | \\u0022 |\n| \\\' | 单引号（Single Quote） | \\u0027 |\n| \\\\ | 反斜杠（Backslash） | \\u005c |', 41, 3),
(268, 27, 23, 1, '另外，需要注意的是，值为换行符（LF）的字符字面值、值为回车（CR）的字符字面值、值为单引号的字符字面值，不能用其 Unicode 码的转义序列来表示，只能用其转义序列“\\n”“\\r”“\\\'”表示。通过 JShell 验证的结果如图 2-20 所示。（图 2-20　转义序列 \\n、\\r、\\\' 的验证：值为 LF、CR、单引号的字符字面值不能用 Unicode 码转义序列表示）', 41, 4),
(269, 27, 23, 1, '在输出语句中，可以使用转义序列。图 2-21 所示的例子展示了转义序列 \\\'、\\t 的使用。在双引号内，可以直接使用单引号，也可以使用转义序列的单引号。例如，例子的第一条输出语句表示单引号时，未使用转义字符。例子的第二条输出语句表示单引号时，使用转义字符 \\\'。这两条输出语句的输出结果一致。对于在上面两条语句中出现的两个 \\t，其处理方式为：第一个 \\t 被当作一个转义序列处理，代表水平制表符，表示占据 8 个字符的位置。由于在 \\t 之前已经有 6 个字符了，所以后补 2 个空格；第二个 \\t 之前有一个 \\，于是 \\t 中的反斜杠被转义成普通的反斜杠，不再是转义字符，因此 \\t 在字符串中表示反斜杠字符和字符 t，变成普通反斜杠 \\ 加上字符 t 输出。第三条语句的 \\t 输出了 8 个空格，因为前面的字符个数有 8 位，制表符 \\t 开始第二个制表位，需要补足 8 个位置，所以输出了 8 个空格。通常，\\t 占 8 列，并且，\\t 开始占据的初始位置是第 8*n 列（n 从 0 开始，列的编号也从 0 开始）。因此，第一个 \\t 的初始位置是第 0 列，第二个 \\t 的初始位置是第 8 列，依次类推。（图 2-21　转义序列 \\\' 与 \\t 的使用：三条输出语句展示单引号与制表符 \\t 的处理方式）', 41, 5),
(270, 27, 23, 1, '另外，在给字符变量赋值时，使用转义序列双引号和直接用双引号，效果一样，如图 2-22 所示。（图 2-22　使用转义序列双引号与直接用双引号给字符变量赋值的效果一样）', 42, 1),
(271, 28, 24, 1, '布尔类型是指仅有值为 true 或 false 的数据类型，其声明的变量是一个具有值为 true 或 false 的变量，使用关键字 boolean 声明，占用 1 个字节。布尔类型声明的变量可简称为布尔变量。布尔变量的声明如下所示：', 42, 2),
(272, 28, 24, 1, 'boolean 变量名;', 42, 3),
(273, 28, 24, 1, '布尔类型的字面值只有两个，即 true 和 false，这两个字面值不能用于标识符。布尔类型表达了逻辑上的真（true）与假（false）。例如，数学表达式 5>3 的结果为真（true），5<3 的结果为假（false）。Java 语言对两个数的大小比较与数学符号一致，如图 2-23 所示。（图 2-23　true、false 使用示例：在 JShell 中执行 System.out.println(5>3) 输出 true，执行 System.out.println(5<3) 输出 false）', 42, 4),
(274, 29, 25, 1, '一种数据类型是可以向另一种数据类型转换的，称为数据类型转换（简称类型转换）。例如，一个整数 3 可以转换成一个浮点数 3.0d，一个浮点数 2.0f 可以转换成一个整数 2。另外，在赋值语句中，可以将某种数据类型的表达式赋值给另一种数据类型的变量。这里的表达式既可以是一个字面值，也可以是一个变量或含有运算符的表达式。', 42, 5),
(275, 29, 25, 1, '本节以赋值运算为例，结合 7 种基本数据类型，介绍 Java 语言的数据类型转换规则。其中 6 种数值类型 byte、short、int、long、float、double，表示数的范围是越来越大的。在 Java 语言中，总是可以将一种数值类型的数值直接赋值给支持更大范围数值类型的变量，这种类型转换称为隐式类型转换。隐式类型转换不会造成精度损失。例如：', 42, 6),
(276, 29, 25, 1, '整型字面值 20 是 int 型，int 型的范围低于 double 型，因此，可以直接将 20 转变成 20.0，再赋值给 double 型变量 d1。', 43, 1),
(277, 29, 25, 1, '反过来，如果把一个范围大的数值类型数值赋给一个范围小的数值类型变量，就不能直接赋值，必须要进行显式类型转换，也称为强制类型转换。显式类型转换需要在待转换的表达式中使用一对圆括号，在圆括号内写上转换之后的数据类型。以赋值运算为例，显式类型转换的语法形式如下：', 43, 2),
(278, 29, 25, 1, '目标数据类型 变量名=(目标数据类型)表达式；', 43, 3),
(279, 29, 25, 1, '例如：', 43, 4),
(280, 29, 25, 1, '浮点型字面值 43.2 是 double 型，比 int 型的范围大，在其前面使用 (int) 进行强制转换，变成整型值 43，再赋值给 int 型变量 i1。通过使用显式类型转换，程序员在阅读代码时，可明确知道这里可能存在精度损失。浮点型数值在显式类型转换为整型数值时，直接被截去小数部分。如果需要采用四舍五入的方式取整，则可以采用下面形式的代码：', 43, 5),
(281, 29, 25, 1, '(整型)(浮点类型数值+0.5)', 43, 6),
(282, 29, 25, 1, '例如：', 43, 7),
(283, 29, 25, 1, 'i2 的值为 44。因为表达式 (43.6+0.5) 的值是 44.1，再经过显式类型转换，就变成整数 44 了，最后再给 i2 赋值。', 43, 8),
(284, 29, 25, 1, '字符类型 char 也是一种数值类型，它可以与上面 6 种数值类型之间进行相互转换。然而，char 是一种无符号整型数，其取值范围大小与 byte、short 没有明显的大小之分。因此，在相互转换时，字符类型有一些特殊之处。', 43, 9),
(285, 29, 25, 1, '下面具体叙述字符类型与其他基本数据类型的转换。', 43, 10),
(286, 29, 25, 1, '（1）字符类型表示的数值范围，低于 3 种数值类型（long、float、double）所支持的数值范围，因此，字符类型与这 3 种数值类型的类型转换与前述规则一致。', 43, 11),
(287, 29, 25, 1, '例如，下面一段代码：', 43, 12),
(288, 29, 25, 1, '（2）byte、short、int 类型与字符类型在进行相互转换时，赋值语句右边的表达式值或字面值只要不超出转换后数据类型的取值范围，可以直接相互赋值，即隐式转换。如果赋值语句右边的字面值超出转换后数据类型的取值范围，就需要进行显式类型转换。', 43, 13),
(289, 29, 25, 1, '例如，下面一段代码：', 43, 14),
(290, 29, 25, 1, '前 4 行代码都是合法的隐式转换，字符类型和 3 种整型类型相互转换。第 5 行声明和初始化字符变量 c2 时，赋值运算符右边的整型字面值超出 char 类型的取值范围，编译器报错。同样地，第 6、7 行 i8、i9 的声明和定义也会产生语法错误，这是由于字符所代表的整型数值超出了赋值运算符左边变量所能存储的数值范围。第 8~10 行的赋值运算，编译器也会报错。编译器报错背后也是存在合理逻辑的，对于第 8、9 行，变量 i5、i6 表示的正整数值虽然没有超出字符类型的取值范围，但是 i5、i6 可能取值为负数，而字符类型是无符号整数。对于第 10 行，变量 i7 无论取正整数或负整数都有可能超出字符类型的取值范围。', 44, 1),
(291, 29, 25, 1, '对于上述编译器报错的第 5~10 行语句，必须使用显式类型转换，编译器才不会报错，即', 44, 2),
(292, 29, 25, 1, '需要注意的是，布尔类型不能与其他基本数据类型进行相互的数据类型转换。例如，下面的一段代码：', 44, 3),
(293, 30, 26, 1, 'Java 语言使用 System.out 表示标准输出设备，可以在控制台（Console）上用方法 System.out.print() 输出信息，或用 System.out.println() 输出一行信息。而要从控制台输入信息，需要使用 java.util.Scanner 类和表示标准输入设备的 System.in 对象。默认情况下，标准输出设备是显示器，标准输入设备是键盘。', 44, 4),
(294, 30, 26, 1, '为了从键盘输入数据，首先需要使用 Scanner 类创建一个 Scanner 对象，以读取来自 System.in 的输入，其形式如下：', 44, 5),
(295, 30, 26, 1, '该语句表示创建了一个 Scanner 对象，并将该对象的引用赋值给 input 变量。Scanner input 表示声明 input 是一个 Scanner 类型的对象，new Scanner(System.in) 表示创建了一个 Scanner 对象，该对象能从标准输入设备（System.in）——键盘获取信息。在该语句中，Scanner 对象的名称 input 也可以定义为其他合法的标识符。', 44, 6),
(296, 30, 26, 1, '其次，Scanner 对象调用 nextXXX 方法以获取输入数据。下面以获取 int 型和 double 型数据为例进行说明，形式如下：', 44, 7),
(297, 30, 26, 1, 'Scanner 对象获取输入数据的 nextXXX 方法及其说明如表 2-6 所示。', 44, 8),
(298, 30, 26, 1, '表 2-6　Scanner 对象获取输入的 nextXXX 方法及其说明\n| 方法 | 说明 |\n| --- | --- |\n| nextByte() | 获取一个 byte 型值 |\n| nextShort() | 获取一个 short 型值 |\n| nextInt() | 获取一个 int 型值 |\n| nextLong() | 获取一个 long 型值 |\n| nextFloat() | 获取一个 float 型值 |\n| nextDouble() | 获取一个 double 型值 |\n| next() | 获取以空白字符结束（'' ''、''\\t''、''\\f''、''\\r''、''\\n''）的字符串 |\n| nextLine() | 获取一整行文本作为一个字符串 |', 45, 1),
(299, 30, 26, 1, '下面通过一个示例，演示 Scanner 对象的使用。该示例要求：输入一个长方形的宽和高，计算其面积。其代码如程序清单 2-3 所示。', 45, 2),
(300, 30, 26, 1, '执行及结果（　表示输入回车键，由键盘输入的值用浅灰色背景标注）：', 45, 3),
(301, 30, 26, 1, '程序清单 2-3 的第 1 行语句是一个导入（import）语句，Scanner 类在 java.util 包中，util 是 utility（实用工具）的简写。第 4 行语句创建了一个 Scanner 对象。第 5 行语句在控制台上显示提示信息“请输入长方形的宽 cm(如，12.5)：”，这种提示能友好地引导用户与计算机进行交互。因此，在编程时，程序应该对用户友好，在希望用户输入数据的时候，应具有明确的提示信息。第 6 行输入一个浮点数。用户可以通过键盘输入一个浮点数，如 13.6，然后按下回车键。这时，程序会读取该数值赋值给变量 width。第 7、8 行代码与第 5、6 行类似。第 10 行代码计算长方形面积（Java 语言用 * 表示乘法运算）。第 11 行是在控制台上显示长方形面积的信息，并保留到小数点后两位，System.out.printf() 在 2.5.7 节具体介绍。', 45, 4),
(302, 30, 26, 1, '程序清单 2-3 的示例引入了导入语句的概念。Java 语言提供了两种类型的导入语句：特定导入（Specific Import）和通配符导入（Wildcard Import）。特定导入是在导入语句中指定单个具体的类。例如，下面的语句就是特定导入，仅仅导入了一个类 Scanner。', 46, 1),
(303, 30, 26, 1, '通配符导入是通过使用星号 * 作为通配符（Wildcard Character），导入一个包中所有的类。例如，下面的语句导入包 java.util 中的所有类：', 46, 2),
(304, 30, 26, 1, '导入语句只是告诉 Java 编译器在哪里能够找到程序中需要用到的类，并不是直接将包中所有类的信息加载到 JVM 中。只有在程序中用到相应的类时，这些类才会被 JVM 加载。另外，特定导入和通配符导入在性能上没有什么差别。', 46, 3),
(305, 30, 26, 1, '下面给出一个从键盘连续读取多个输入的例子。该示例要求用户连续输入 3 个单精度浮点数，并计算它们的平均值，平均值保留小数点后两位。', 46, 4),
(306, 30, 26, 1, '在连续输入时，输入项之间可以采用空白字符（空格键、Tab 键、回车键）分隔。一个空格键可以输入一个空格字符。本书用 □ 表示输入空格字符，用 Tab 表示输入 Tab 键。', 46, 5),
(307, 30, 26, 1, '该示例的代码如程序清单 2-4 所示。', 46, 6),
(308, 30, 26, 1, '程序清单 2-4 的第 6~8 行要求连续输入 3 个浮点数。第一种输入方式，前两个输入项之后输入空格字符（Space 键），最后一个输入项之后按下回车键。第二种输入方式，每个输入项之后均按下回车键。第三种输入方式，前两个输入项之后，按下 Tab 键，最后一个输入项之后按下回车键。在执行程序时，输入的是整数 3、4、5，这些整数被自动转换为浮点数 3.0f、4.0f、5.0f，分别赋给 3 个单精度浮点类型变量。另外，在执行程序时，输入带小数点的数（只要在单精度浮点类型的取值范围内）都是正确的。因此，当要求输入浮点数时，既可以输入整数，也可以输入浮点类型取值范围的带小数点的数。', 46, 7),
(309, 30, 26, 1, '程序 3 次运行的结果分别如下所示（3 种不同输入方式）：', 46, 8),
(310, 30, 26, 1, '下面一个示例展示 next()、nextLine() 方法的使用以及字符的输入，具体代码如程序清单 2-5 所示。字符的输入需要借助 next()、nextLine() 方法，然后，使用 charAt() 取出一个字符。', 47, 1),
(311, 30, 26, 1, '注意：为了避免输入错误，一定不要在方法 nextByte()、nextShort()、nextInt()、nextLong()、nextFloat()、nextDouble()、next() 之后直接调用方法 nextLine() 输入值。为了在上述方法调用之后使用方法 nextLine()，可以通过使用一个冗余的 nextLine() 处理完分隔标记之后，再次使用 nextLine()。例如，程序清单 2-5 第 14 行使用 input.nextLine() 处理前面输入的分隔标记，第 17 行就可以再次使用方法 nextLine() 进行输入了。具体原因，见 11.3.1 节。', 47, 2),
(312, 30, 26, 1, '运行结果如下：', 48, 1),
(313, 30, 26, 1, '程序清单 2-5 第 8、9、10 行使用 next() 输入 3 个以空白字符结束（'' ''、''\\t''、''\\f''、''\\r''、''\\n''）的字符串，换言之，输入的字符串不含空白字符。执行这 3 行语句，在输入时，可以按下空格键或 Tab 键或回车键分隔 3 个字符串。第 17 行 nextLine() 方法输入一整行文本作为一个字符串，可以包含空白字符，即读取以按下回车键为结束标志的字符串。第 21 行使用 nextLine() 输入一个字符串，第 22 行取出字符串的第一个字符赋给字符变量 ch1。第 21 行语句在执行时也可以只输入单个字符，即使输入一个字符串，在第 22 行也明确指定了取第一个字符。', 48, 2),
(314, 31, 27, 1, 'Java 语言提供了 System.out.printf() 方法在控制台上以某种指定的格式输出信息，printf 中的 f 表示格式（Format）。在一些场景中，数据需要以某种具体格式来输出。例如，给定一个存款金额、一个存款年利率，计算每年的利息，其代码片段如下：', 48, 3),
(315, 31, 27, 1, '输出结果是：', 48, 4),
(316, 31, 27, 1, '因为利息是货币，所以希望计算出来的利息数值保留到小数点后两位数字。为此，可以如下编写代码：', 48, 5),
(317, 31, 27, 1, '输出结果是：', 48, 6),
(318, 31, 27, 1, '然而，输出结果还是没有做到保留小数点后两位（即 155.81），上面的代码通过数据类型转换强行将小数点两位之后的数字截掉，没有进行四舍五入。', 48, 7),
(319, 31, 27, 1, '为了解决上面的问题，用户可以使用 System.out.printf 方法，代码片段如下：', 48, 8),
(320, 31, 27, 1, '代码中的 %6.2f 对输出格式进行了控制，输出结果是：', 49, 1),
(321, 31, 27, 1, '方法 printf() 的调用形式如下：', 49, 2),
(322, 31, 27, 1, 'System.out.printf(格式字符串, 输出项 1, 输出项 2, ……, 输出项 k);', 49, 3),
(323, 31, 27, 1, '格式字符串是一个由多个字符串子串和格式限定符构成的字符串。如前面示例的语句：', 49, 4),
(324, 31, 27, 1, '字符串子串有“利息是”、“元”，格式限定符是“%6.2f”，这个格式限定符各部分含义如下所示：（图　%6.2f 格式限定符各部分含义示意图：% 为格式限定符的开始标志，6 为域宽，2 为精度即小数点后保留的位数，f 为转换码，表示输出项是一个浮点数）', 49, 5),
(325, 31, 27, 1, '因此，%6.2f 表示输出项是一个浮点数，域宽是 6，小数点后保留 2 位。', 49, 6),
(326, 31, 27, 1, '一个格式限定符指定了对应的输出项该如何显示。输出项可以是数值、字符、布尔值或字符串等。格式限定符是以百分号 % 开头的转换码，一些常见的、简单的格式限定符如表 2-7 所示。', 49, 7),
(327, 31, 27, 1, '表 2-7　常用的简单格式限定符\n| 限定符 | 描述 |\n| --- | --- |\n| %b | 输出布尔值 true 或 false |\n| %c | 输出一个字符 |\n| %d | 输出一个十进制整数 |\n| %f | 输出一个浮点数 |\n| %e | 输出科学记数法形式的数 |\n| %s | 输出一个字符串 |', 49, 8),
(328, 31, 27, 1, '下面的例子进一步演示了 printf 方法的使用，代码片段如下：', 49, 9),
(329, 31, 27, 1, '输出结果如下：', 49, 10),
(330, 31, 27, 1, '通过上面的例子可以看出，格式限定符与输出项必须在顺序、数量和数据类型上匹配。例如，name 的格式限定符是 %s，输出项“存款”的格式限定符是 %f，输出项“年数”的格式限定符是 %d。从上面的例子中可以看出，%f 在没有指定精度时，默认输出小数点后 6 位。', 49, 11),
(331, 31, 27, 1, '对于每一个格式限定符，还可以指定其域宽和精度。假设 m、n 表示两个正整数，输出一个浮点数的一般形式如下：', 49, 12),
(332, 31, 27, 1, '%m.nf、%-m.nf、%m.ne、%-m.ne', 49, 13),
(333, 31, 27, 1, '对于非浮点数，一般只能指定其域宽，假设以 X 表示非浮点数的转换码，一般形式如下：', 49, 14),
(334, 31, 27, 1, '%mX、%-mX', 49, 15),
(335, 31, 27, 1, '域宽可以是正数，也可以是负数。正数表示右对齐，负数表示左对齐。域宽低于输出项的实际宽度时，按输出项的实际宽度输出。表 2-8 给出了一些例子。', 50, 1),
(336, 31, 27, 1, '表 2-8　指定域宽、精度的例子\n| 举例 | 描述 |\n| --- | --- |\n| %3c | 域宽为 3，右对齐，输出一个字符并在其左边补 2 个空格 |\n| %-3c | 域宽为 3，左对齐，输出一个字符并在其右边补 2 个空格 |\n| %6b | 域宽为 6，右对齐，输出 true 并在其左边补 2 个空格，或者输出 false 在左边补 1 个空格 |\n| %-6b | 域宽为 6，左对齐，输出 true 并在其右边补 2 个空格，或者输出 false 在右边补 1 个空格 |\n| %5d | 域宽为 5，右对齐，输出项位数如果不小于 5，则以实际域宽输出所有位数；如果位数小于 5，则在输出数字的左边补空格，使该项的宽度为 5 |\n| %-5d | 域宽为 5，左对齐，输出项位数如果不小于 5，则以实际域宽输出所有位数；如果位数小于 5，则在输出数字的右边补空格，使该项的宽度为 5 |\n| %6.2f | 域宽为 6，右对齐，输出的浮点数项位数如果不小于 6，则以实际域宽输出所有位数；如果位数小于 6，则在输出数字的左边补空格，使该项的宽度为 6 |\n| %-6.2f | 域宽为 6，左对齐，输出项位数如果不小于 6，则以实际域宽输出所有位数；如果位数小于 6，则在输出数字的右边补空格，使该项的宽度为 6 |\n| %6.2e | 域宽为 6，右对齐，以科学记数法方式显示输出项，输出项位数如果不小于 6，则以实际域宽输出所有位数；如果位数小于 6，则在输出数字的左边补空格，使该项的宽度为 6 |\n| %-6.2e | 域宽为 6，左对齐，以科学记数法方式显示输出项，输出项位数如果不小于 6，则以实际域宽输出所有位数；如果位数小于 6，则在输出数字的右边补空格，使该项的宽度为 6 |\n| %10s | 域宽为 10，右对齐，输出项字符个数如果不小于 10，则以实际域宽输出所有字符；如果字符个数小于 10，则在输出字符的左边补空格，使该项的宽度为 10 |\n| %-10s | 域宽为 10，左对齐，输出项字符个数如果不小于 10，则以实际域宽输出所有字符；如果字符个数小于 10，则在输出字符的右边补空格，使该项的宽度为 10 |', 50, 2),
(337, 31, 27, 1, '下面的代码展示了域宽的使用：', 50, 3),
(338, 31, 27, 1, '其输出结果如下（□ 表示空格字符）：', 50, 4),
(339, 31, 27, 1, '这里的第一个输出项是整数 12，实际域宽为 2，指定域宽为 4，左对齐，右补两个空格。第二个输出项是字符串 " Hello"，有 5 个字符，指定域宽为 3，按实际宽度输出所有字符。第三个输出项是双精度浮点数 123.5678，实际宽度为 8，截取小数点后两位之后的实际宽度为 6，而指定域宽为 7，左对齐，右补一个空格。第四个输出项是双精度浮点数 1234.5678，写成科学记数法形式即为 1.2345678e+3，保留小数点后两位，变成 1.23e+03，实际宽度为 8，右对齐，左补两个空格。', 50, 5),
(340, 31, 27, 1, '如果需要在数字前面用数字 0 作为前导而不是使用空格填充，可以在数字限定符前面添加 0。例如，下面的代码：', 50, 6),
(341, 31, 27, 1, '其输出结果是：', 50, 7),
(342, 31, 27, 1, '方法 printf 还可以显示带有千位分隔符的数字，只需要在格式限定符的开始标志 % 之后添加一个逗号。例如，下面的代码：', 51, 1),
(343, 31, 27, 1, '其输出结果是：', 51, 2),
(344, 31, 27, 1, '千位分隔符也是占据域宽的，例如，第一个输出项实际域宽为 7，加上 2 个千位分隔符，再加 1 个前导 0，宽度正好为 10。另外，千位分隔符是不能和科学记数法一起使用的。', 51, 3),
(345, 31, 27, 1, '下面一个示例使用 printf() 方法进行表格式输出。通过一个表格方式输出 11、12、13 这 3 个数及其平方、立方，输出格式如下所示：', 51, 4),
(346, 31, 27, 1, '该示例的代码如程序清单 2-6 所示。', 51, 5),
(347, 31, 27, 1, '运行结果为：', 51, 6),
(348, 31, 27, 1, '程序清单 2-6 第 3 行输出表头，第 4~6 行输出表格的具体内容。每一行每一项的域宽都是 8。转义字符 \\n 表示换行。', 51, 7),
(349, 32, NULL, 1, 'Java 语言提供了丰富的运算符，包括算术运算符、关系运算符、逻辑运算符、赋值与增强赋值运算符、位运算符等。Java 语言使用的运算符与 C 语言、C++ 语言基本相同。', 51, 8),
(350, 33, 29, 1, '算术运算符有加减运算符、乘除运算符、取余运算符、自增自减运算符等。下面首先分别介绍这几种运算符，然后介绍算术混合运算的数据类型转换。', 52, 1),
(351, 33, 29, 1, '1. 加减运算符\n加法运算符是加号 +，减法运算符是减号 -，和数学上的表达一样。它们都是二元运算符，连接两个操作数，结合方向是从左到右。例如，表达式 2+4-3+5，从左到右，先计算 2+4 得到 6，再计算 6-3 得到 3，最后计算 3+5 得到 8。加减运算符的操作数是数值型数据。加减运算符的优先级相同。', 52, 2),
(352, 33, 29, 1, '2. 乘除运算符\n乘法运算符是星号 *，除法运算符是斜杠 /，和数学的乘除符号不一样。这两个运算符也都是二元运算符，连接两个操作数，结合方向是从左到右。例如，表达式 4*3/2*2，从左到右，先计算 4*3 得到 12，再计算 12/2 得到 6，最后计算 6*2 得到 12。乘除运算符的操作数也是数值型数据。当除法的两个操作数都是整数时，除法运算的结果也是整数，当出现不能整除的情况时，小数部分会被舍去。例如，7/2 的结果是 3 而不是 3.5，-7/2 的结果是 -3 而不是 -3.5。如果除法运算需要保留小数部分，其中至少一个操作数必须是浮点数，例如，5.0/2、5/2d、5d/2 等表达式的计算结果都是 2.5。', 52, 3),
(353, 33, 29, 1, '和数学运算一样，乘除运算符的优先级相同，且高于加减运算。', 52, 4),
(354, 33, 29, 1, '3. 取余运算符\n取余运算符是百分号 %，取余运算也称为求余运算、模运算、取模运算，是一个二元运算符，连接两个操作数，左边的操作数是被除数，右边的操作数是除数，运算结果是执行除法后得到的余数。结合方向也是从左往右，取余运算符的优先级与乘除运算符相同。例如，表达式 24%13*2%7，从左到右，先算 24%13 得到 11，再算 11*2 得到 22，最后再算 22%7 得到 1。', 52, 5),
(355, 33, 29, 1, '取余运算符 % 的操作数既可以是正整数，也可以用于浮点数和负整数、字符。只有当被除数是负数时，余数才是负的。字符参与取余运算时，先被转换为对应的整数，再参与运算。表 2-9 给出了一些取余运算的示例。+、- 除了作为二元运算符表示加法和减法外，还可以作为一元运算符，表示正号、负号。例如，+10.2 与 10.2 是一样的，-10.2 表示是负数。它们的优先级高于乘除运算。', 52, 6),
(356, 33, 29, 1, '表 2-9　取余运算举例\n| 示例 | 结果 | 示例 | 结果 | 示例 | 结果 |\n| --- | --- | --- | --- | --- | --- |\n| 25%12 | 1 | ''A''%7 | 2 | 23.5%4.5 | 1.0 |\n| -25%12 | -1 | -''A''%7 | -2 | 23.5%4 | 3.5 |\n| -25%-12 | -1 | -''A''%-7 | -2 | 23.8%3.6 | 2.2 |\n| 25%-12 | 1 | ''A''%-7 | 2 | -23.8%3.6 | -2.2 |', 52, 7),
(357, 33, 29, 1, '取余运算在程序设计中是很有用的。例如，判断一个整数 d 是否为偶数，可以用 d%2 的结果是否为零来判断。如果 d%2 的运算结果为零，那么 d 是偶数，否则 d 是奇数。再比如，假设 1 月 1 日是星期三，那么 1 月 25 日是星期几？可以使用下面的表达式进行求解：', 52, 8),
(358, 33, 29, 1, '(25-1)%7　//月份相同，用指定日 25 减去起始日 1，算出天数，再对 7 取余', 53, 1),
(359, 33, 29, 1, '其结果是 3。假如表达式的结果是 0，那么说明和 1 月 1 日一样，指定日对应星期三。而当余数是 3 时，就往后加 3，变成星期六。于是，1 月 25 日是星期六。', 53, 2),
(360, 33, 29, 1, '还可以让余数与星期几有对应关系，即余数 0 对应星期日，余数 1~6 分别对应星期一至星期六，可以使用下面的表达式：', 53, 3),
(361, 33, 29, 1, '(3+25-1)%7', 53, 4),
(362, 33, 29, 1, '其结果是 6，表示星期六。', 53, 5),
(363, 33, 29, 1, '下面的示例通过除法和取余运算求出一个 3 位数的百位数字、十位数字和个位数字。该示例的代码如程序清单 2-7 所示。', 53, 6),
(364, 33, 29, 1, '运行结果如下：', 53, 7),
(365, 33, 29, 1, '使用除法和取余运算，程序清单 2-7 第 8 行计算得出个位数字，第 10 行计算得出十位数字，第 12 行计算得出百位数字。', 53, 8),
(366, 33, 29, 1, '4. 自增自减运算符\n自增运算符是 ++，自减运算符是 --。它们都是一元运算符，其操作数是数值型变量。在许多编程任务中，常常需要对变量加 1 或减 1，引入这两个操作符，可以简便一些。例如，下面的代码：', 54, 1),
(367, 33, 29, 1, '其中，i++ 读作“i 加加”，j-- 读作“j 减减”。这两个运算符都是放在变量名称之后，因此，它们分别被称为后置自增运算符和后置自减运算符。这两个运算符也可以放在变量名称前面，例如，下面的代码就是前置的例子：', 54, 2),
(368, 33, 29, 1, '其中，++i 读作“加加 i”，--j 读作“减减 j”。它们分别被称为前置自增运算符和前置自减运算符。', 54, 3),
(369, 33, 29, 1, '在上面的两个示例中，仅仅进行了自增自减，并且单独作为一条语句，前置自增与后置自增、前置自减与后置自减效果一样。然而，当自增、自减运算符用于表达式中时，它们的作用就会不同。表 2-10 举例描述了前置、后置的区别（变量 v 表示一个数值类型的变量）。', 54, 4),
(370, 33, 29, 1, '表 2-10　自增、自减运算符\n| 运算符 | 名称 | 描述 | 示例（假设 j=2） |\n| --- | --- | --- | --- |\n| v++ | 后置自增 | 将 v 加 1，在表达式中使用 v 原来的值 | int i = j++;    //i 为 2, j 为 3 |\n| ++v | 前置自增 | 将 v 加 1，在表达式中使用 v 新的值 | int i = ++j;    //i 为 3, j 为 3 |\n| v-- | 后置自减 | 将 v 减 1，在表达式中使用 v 原来的值 | int i = j--;    //i 为 2, j 为 1 |\n| --v | 前置自减 | 将 v 减 1，在表达式中使用 v 新的值 | int i = --j;    //i 为 1, j 为 1 |', 54, 5),
(371, 33, 29, 1, '自增、自减运算符的优先级高于乘除运算。如下的代码段：', 54, 6),
(372, 33, 29, 1, 'j 的值是 30。表达式 5*++i 的运算是从左向右进行的，由于自增运算符的优先级高于乘除运算，所以，先计算 ++i 得到的值是 6，且用新的值 6 参与乘法运算，得到结果 30。', 54, 7),
(373, 33, 29, 1, '下面的代码段：', 54, 8),
(374, 33, 29, 1, 'j 的值是 25。表达式 5*i++ 的运算是从左向右进行的，i++ 的运算是先取出 i 的值 5 参与乘法运算，再自增 1，因此，j 的值是 25，i 的值是 6。', 54, 9),
(375, 33, 29, 1, '下面的示例演示在同一个表达式中对同一个变量自增、自减，代码如下：', 54, 10),
(376, 33, 29, 1, '运行结果如下：', 54, 11),
(377, 33, 29, 1, '首先执行 x++，取出 x 的原值 2 参与乘法运算，并将 x 变为 3，再执行 ++x，将 x 的值变为 4，参与加法运算。于是，y 的值为 2*2+4，得到 8。', 54, 12),
(378, 33, 29, 1, '下面是一个更为复杂的示例，代码如程序清单 2-8 所示。', 54, 13),
(379, 33, 29, 1, '运行结果如下：', 55, 1),
(380, 33, 29, 1, '程序清单 2-8 第 4 行在计算时，加法运算从左向右进行。先处理第一项 x++，取出值 1 参与加法运算，x 变为 2；接着，处理第二项 ++x，x 的值先增加 1 变为 3，再参与乘法运算，变为 3*3，得到 9；然后，处理第三项 x++，取出值 3 参与乘法运算，x 再自增 1 变为 4，于是 3*2，得到 6；最后，y 的值为 1+9+6，得到 16。同理第 7 行也是按顺序从左到右处理每一项自增运算。', 55, 2),
(381, 33, 29, 1, '5. 算术混合运算\n通过使用 Java 语言提供的算术运算符，程序员能够很方便地将数学算术表达式转换为 Java 语言中的表达式。例如，算术表达式', 55, 3),
(382, 33, 29, 1, '（此处为数学算术表达式的图片）', 55, 4),
(383, 33, 29, 1, '可以转换成如下所示的 Java 表达式：', 55, 5),
(384, 33, 29, 1, '（此处为对应的 Java 表达式的图片）', 55, 6),
(385, 33, 29, 1, '在进行算术混合运算时，Java 语言的算术运算规则与数学上的算术运算规则一致。首先计算的是括号中包含的运算符。括号可以嵌套，先计算最内层括号，再逐步向外。其次，当表达式有多个运算符时，运算符的优先级用来确定计算的顺序：乘、除、取余运算符优先于加、减运算符，按照从左到右的顺序执行。例如，下面的表达式：', 55, 7),
(386, 33, 29, 1, 'Java 语言在计算算术表达式时，两个不同类型的操作数也能进行二元运算，Java 语言提供了如下数据类型转换规则：', 55, 8),
(387, 33, 29, 1, '（1）如果一个操作数是双精度浮点数（double 类型），那么另一个操作数也被转换为双精度浮点数，再进行运算，运算结果也是双精度浮点类型。例如，7.0/2 的结果是 3.5d。', 55, 9),
(388, 33, 29, 1, '（2）如果操作数的最高精度是单精度浮点类型（float 类型），则另一个操作数也被转换为单精度浮点数，再进行运算，运算结果也是单精度浮点类型。例如，7.0f/2 的结果是 3.5f。', 56, 1),
(389, 33, 29, 1, '（3）如果操作数的最高精度是长整型（long 类型），则另一操作数也被转换为长整型数，再进行运算，运算结果也是 long 类型。例如，10L+''A'' 的结果是 75L。', 56, 2),
(390, 33, 29, 1, '（4）如果操作数的最高精度是 int 类型或低于 int 类型，则所有操作数均被转换为 int 类型整数，再进行运算，运算结果也是 int 类型。例如，(byte)5+''A'' 的值是 70，7/2 的结果是 3。', 56, 3),
(391, 33, 29, 1, '下面的示例（见程序清单 2-9）利用数学公式“华氏温度 = 32+9.0/5×摄氏温度”，将摄氏温度转换为华氏温度。', 56, 4),
(392, 33, 29, 1, '运行结果如下：', 56, 5),
(393, 33, 29, 1, '第 9 行在将数学公式转换为 Java 表达式时使用的是 9.0/5，而不是 9/5。如果使用 9/5，温度转换就会出现错误。这是因为 9.0/5 的运算结果是 1.8d，而 9/5 的结果是 1。因此，在将数学中的除法运算转换为 Java 代码时，应注意数值类型。', 56, 6),
(394, 34, 30, 1, '在 Java 语言中，运算符 +、-、*、/、% 可以结合赋值运算符使用，形成增强赋值运算符。一些常见的增强赋值运算符如表 2-11 所示。', 56, 7),
(395, 34, 30, 1, '表 2-11　增强赋值运算符\n| 运算符 | 名称 | 示例 | 等价表达式 |\n| --- | --- | --- | --- |\n| += | 加法赋值运算符 | i+=5 | i=i+5 |\n| -= | 减法赋值运算符 | i-=5 | i=i-5 |\n| *= | 乘法赋值运算符 | i*=5 | i=i*5 |\n| /= | 除法赋值运算符 | i/=5 | i=i/5 |\n| %= | 取余赋值运算符 | i%=5 | i=i%5 |', 56, 8),
(396, 34, 30, 1, '增强赋值运算符的优先级和赋值运算符一样，优先级在运算符中是最低的，而且也是从右向左结合的。与赋值运算符有区别的地方在于，增强赋值运算符支持一种自动的强制类型转换形式。例如，下面的代码存在错误：', 57, 1),
(397, 34, 30, 1, '然而，在使用增强赋值运算符后，下面的代码是正确的：', 57, 2),
(398, 34, 30, 1, '在 Java 语言中，x1 op= x2 形式的增强赋值表达式可等价转换为 x1 = (T)(x1 op x2)，这里 x1、x2 表示两个操作数，T 表示 x1 的数据类型，op 表示运算符 +、-、*、/、% 中的一个。', 57, 3),
(399, 35, 31, 1, '关系运算符（Relation Operator）也称为比较运算符（Comparison Operator），是二元运算符，用于比较两个值的关系。关系运算符的运算结果是布尔类型的值，即 true 或 false。当关系运算符对应的关系成立时，运算结果为 true，否则为 false。例如，2<3 的结果是 true，2>3 的结果是 false。', 57, 4),
(400, 35, 31, 1, 'Java 语言提供了 6 种关系运算符，如表 2-12 所示（假设小刘的年龄 age=15）。', 57, 5),
(401, 35, 31, 1, '表 2-12　关系运算符\n| 关系运算符 | 名称 | 数学符号 | 示例 | 结果 |\n| --- | --- | --- | --- | --- |\n| > | 大于 | ＞ | age>18 | false |\n| >= | 大于等于 | ≥ | age>=18 | false |\n| < | 小于 | ＜ | age<18 | true |\n| <= | 小于等于 | ≤ | age<=18 | true |\n| == | 等于 | ＝ | age==18 | false |\n| != | 不等于 | ≠ | age!=18 | true |', 57, 6),
(402, 36, 32, 1, '逻辑运算符（Logical Operator）也称为布尔运算符（Boolean Operator），用于计算布尔值，运算结果产生新的布尔值（true 或 false）。逻辑运算符有 4 种，如表 2-13 所示。', 57, 7),
(403, 36, 32, 1, '表 2-13　逻辑运算符\n| 运算符 | 名称 | 说明 |\n| --- | --- | --- |\n| ! | 非 | 逻辑非 |\n| && | 与 | 逻辑与 |\n| \|\| | 或 | 逻辑或 |\n| ^ | 异或 | 逻辑异或 |', 57, 8),
(404, 36, 32, 1, '表 2-14 给出了逻辑非运算符 ! 的真值表，非运算符是一元运算符，逻辑非运算只有一个布尔类型操作数，对操作数的值取反。', 57, 9),
(405, 36, 32, 1, '表 2-14　运算符 ! 的真值表\n| p | !p | 示例（一个人的身高 height=180 cm，体重 weight=160 斤） |\n| --- | --- | --- |\n| true | false | height>175 为 true，!(height>175) 为 false |\n| false | true | weight>170 为 false，!(weight>170) 为 true |', 58, 1),
(406, 36, 32, 1, '表 2-15 给出了逻辑与运算符的真值表，与运算符是二元运算符，逻辑与运算有两个布尔类型操作数，当且仅当两个操作数都为 true 时，运算结果为 true。', 58, 2),
(407, 36, 32, 1, '表 2-15　运算符 && 的真值表\n| p1 | p2 | p1&&p2 | 示例（某人的身高 height=180 cm，体重 weight=160 斤） |\n| --- | --- | --- | --- |\n| true | true | true | height>175 && weight>140 |\n| false | true | false | height>190 && weight>140 |\n| true | false | false | height>175 && weight>170 |\n| false | false | false | height>190 && weight>170 |', 58, 3),
(408, 36, 32, 1, '表 2-16 给出了逻辑或运算符 || 的真值表，或运算符是二元运算符，逻辑或运算有两个布尔类型操作数，当且仅当两个操作数都为 false 时，运算结果为 false。', 58, 4),
(409, 36, 32, 1, '表 2-16　运算符 \|\| 的真值表\n| p1 | p2 | p1\|\|p2 | 示例（某人的身高 height=180 cm，体重 weight=160 斤） |\n| --- | --- | --- | --- |\n| true | true | true | height>175 \|\| weight>140 |\n| false | true | true | height>190 \|\| weight>140 |\n| true | false | true | height>175 \|\| weight>170 |\n| false | false | false | height>190 \|\| weight>170 |', 58, 5),
(410, 36, 32, 1, '表 2-17 给出了逻辑异或运算符 ^ 的真值表，异或运算符是二元运算符，逻辑异或运算有两个布尔类型操作数，当且仅当两个操作数具有不同的布尔值时，运算结果才为 true，p1^p2 等价于 p1 != p2。', 58, 6),
(411, 36, 32, 1, '表 2-17　运算符 ^ 的真值表\n| p1 | p2 | p1^p2 | 示例（某人的身高 height=180 cm，体重 weight=160 斤） |\n| --- | --- | --- | --- |\n| true | true | false | height>175 ^ weight>140 |\n| false | true | true | height>190 ^ weight>140 |\n| true | false | true | height>175 ^ weight>170 |\n| false | false | false | height>190 ^ weight>170 |', 58, 7),
(412, 36, 32, 1, '运算符 && 和 || 被称为短路运算符或懒惰运算符。对于 && 运算符而言，如果第一个操作数为 false，那么无论第二个操作数是什么值，运算结果都为 false。对于 || 运算符而言，如果第一个操作数为 true，那么无论第二个操作数是什么值，运算结果都为 true。因此，Java 语言在处理 &&、|| 运算时，如果出现上面所述情况，就不再考虑第二个操作数的值，直接给出运算结果。', 58, 8),
(413, 37, 33, 1, 'Java 语言提供了一个三元条件运算符“? :”，其一般形式是：', 58, 9),
(414, 37, 33, 1, '布尔表达式 1 ? 表达式 2 : 表达式 3', 59, 1),
(415, 37, 33, 1, '条件运算符的运算规则是：首先，判断布尔表达式 1 的值（true 或 false）。如果值为 true，那么整个条件运算表达式的值为表达式 2 的值；否则，整个条件运算表达式的值为表达式 3 的值。例如，下面的语句：', 59, 2),
(416, 37, 33, 1, '如果 x 为 3，y 为 5，那么 x>y 为 false，条件运算表达式的值为 y，将 y 的值赋给 max，max 为 5。如果 x 为 4，y 为 2，那么 x>y 为 true，条件运算表达式的值为 x，将 x 的值赋给 max，max 为 4。', 59, 3),
(417, 38, 34, 1, '当各种运算符在一起进行混合运算时，如何确定它们的计算顺序呢？例如，下面的语句：', 59, 4),
(418, 38, 34, 1, '它的执行顺序是什么？这就需要弄明白运算符的优先级和结合方向。运算符优先级规则定义了运算符的先后次序。而优先级相同的运算符在一起运算时，它们的结合方向决定了运算符的先后次序。对于二元运算符而言，除了赋值运算符，所有二元运算符都是左结合的（从左向右）。首先，圆括号具有最高优先级，先计算括号中的表达式。其次，算术运算符的优先级高于关系运算符，关系运算符的优先级高于逻辑运算符（除了逻辑非运算符 !），逻辑运算符的优先级高于赋值运算符。运算符优先级如表 2-18 所示，从上至下依次降低，在同一栏的运算符优先级相同。', 59, 5),
(419, 38, 34, 1, '表 2-18　运算符优先级\n| 运算符 | 说明 |\n| --- | --- |\n| expr++（后置自增）、expr--（后置自减） | 算术运算 |\n| ++expr（前置自增）、--expr（前置自减）、+expr（一元加号）、-expr（一元减号）、!（一元逻辑非） | 算术运算与逻辑非运算 |\n| (type) expr（类型转换） | 一元类型转换 |\n| *（乘法）、/（除法）、%（取余） | 算术运算 |\n| +（加法）、-（减法） | 算术运算 |\n| >（大于）、>=（不小于）、<（小于）、<=（不大于） | 关系运算 |\n| ==（相等）、!=（不等于） | 关系运算 |\n| ^（异或） | 逻辑运算 |\n| &&（与） | 逻辑运算 |\n| \|\|（或） | 逻辑运算 |\n| ?:（条件运算符） | 三元运算符 |\n| =、+=、-=、*=、/=、%= | 赋值运算符 |', 59, 6),
(420, 38, 34, 1, '基于运算符优先级和结合方向，下面语句的运算顺序如下。', 59, 7),
(421, 39, NULL, 1, 'Java 语言在 java.lang 包（Java 语言基础包，隐式导入到程序中）中提供了 Math 类，在程序中可以直接使用 Math 类。Math 类提供许多常用的数学函数方法，这些方法可以分为 3 类：三角函数方法（Trigonometric Method）、指数函数方法（Exponent Method）和服务方法（Service Method）。除了这些方法，Math 类还提供了两个很有用的 double 型常量：PI（圆周率 π，约等于 3.1415926）和 E（自然对数的底，约等于 2.71828）。', 60, 1),
(422, 40, 36, 1, 'Math 类提供一系列计算三角函数的方法，这些三角函数方法的返回值均为 double 类型，也都是 public 方法，如表 2-19 所示（本书在表中说明 public 方法时，省略 public 关键字）。', 60, 1),
(423, 40, 36, 1, '表 2-19　Math 类中的三角函数方法\n| 方法 | 描述 |\n| --- | --- |\n| static double sin(double a) | 返回以弧度为单位的角度 a 的三角正弦函数值 |\n| static double cos(double a) | 返回以弧度为单位的角度 a 的三角余弦函数值 |\n| static double tan(double a) | 返回以弧度为单位的角度 a 的三角正切函数值 |\n| static double asin(double a) | 返回以弧度为单位的角度 a 的反三角正弦函数值 |\n| static double acos(double a) | 返回以弧度为单位的角度 a 的反三角余弦函数值 |\n| static double atan(double a) | 返回以弧度为单位的角度 a 的反三角正切函数值 |\n| static double toRadians(double angdeg) | 将以度为单位的角度值 angdeg 转换为以弧度为单位的值，并返回弧度值 |\n| static double toDegrees(double angrad) | 将以弧度为单位的角度 angrad 转换为以度为单位的值，并返回转换后的值 |', 60, 2),
(424, 40, 36, 1, '表 2-19 中的方法 toRadians 的形式参数 angdeg 是以度为单位的角度值，其他所有方法的形式参数都是以弧度为单位的角度值。反正弦 asin、反余弦 acos、反正切 atan 的返回值是 double 类型的、以弧度为单位的角度值。其中，asin 和 atan 的返回值位于 -π/2~π/2 之间，acos 的返回值位于 0~π 之间。π 弧度是 180°，对应的 double 类型值是 3.1415926…。1° 是 π/180 弧度，90° 是 π/2，60° 是 π/3。表 2-20 是三角函数的一些使用示例。', 60, 3),
(425, 40, 36, 1, '表 2-20　三角函数方法使用示例\n| 调用方法 | 说明 |\n| --- | --- |\n| Math.sin(Math.PI/2) | 返回 1.0 |\n| Math.sin(Math.PI/6) | 返回 0.49999999…，相当于 0.5 |\n| Math.cos(Math.PI/2) | 返回 6.123233995736766E-17，相当于 0 |\n| Math.cos(Math.PI/6) | 返回 0.8660254…，相当于 √3/2 |\n| Math.cos(0) | 返回 1.0 |\n| Math.asin(0.5) | 返回 0.523598…，相当于 π/6 |\n| Math.acos(0.5) | 返回 1.047…，相当于 π/3 |\n| Math.acos(-1) | 返回 3.141592653589793，相当于 π |\n| Math.atan(1.0) | 返回 0.7853…，相当于 π/4 |\n| Math.toRadians(90) | 返回 1.57…，相当于 π/2 |\n| Math.toDegrees(Math.PI/6) | 返回 29.999…，相当于 30 |', 61, 4),
(426, 40, 36, 1, '程序清单 2-10 展示的程序对表 2-20 中的结果进行了验证。', 61, 5),
(427, 40, 36, 1, '由于 double 类型数值在计算时存在精度损失问题，所以有些计算结果并不是数学上计算出来的结果。例如，π/6 是 30°，数学计算结果是 30.0，然而程序运算结果是 29.999999999999996。', 62, 6),
(428, 41, 37, 1, 'Math 类提供了 5 个与指数函数有关的方法，如表 2-21 所示。', 62, 1),
(429, 41, 37, 1, '表 2-21　Math 类的指数函数\n| 方法 | 描述 |\n| --- | --- |\n| static double exp(double a) | 返回 e 的 a 次幂（e^a） |\n| static double log(double a) | 返回 a 的自然对数（ln a） |\n| static double log10(double a) | 返回 a 的以 10 为底的对数（log₁₀a） |\n| static double sqrt(double a) | 对于 a≥0 的数字，返回 a 的平方根（√a）；对于 a<0 的数字或 NaN，返回值是 NaN |\n| static double pow(double a, double b) | 返回 a 的 b 次幂（aᵇ） |', 62, 2),
(430, 41, 37, 1, '程序清单 2-11 展示了一个使用指数函数方法的示例。', 62, 3),
(431, 42, 38, 1, 'Math 类的服务方法包括取整、求最大值、求最小值、求绝对值及随机方法。表 2-22 展示了 Math 类的取整方法。', 63, 1),
(432, 42, 38, 1, '表 2-22　Math 类的取整方法\n| 方法 | 描述 |\n| --- | --- |\n| static double ceil(double a) | a 向上取整为它最接近的整数，返回整数的双精度值 |\n| static double floor(double a) | a 向下取整为它最接近的整数，返回整数的双精度值 |\n| static double rint(double a) | a 取整为它最接近的整数，如果 a 与相近的两个整数距离相等，那么返回偶数整数的双精度值 |\n| static int round(float a) | 单精度浮点数 a 四舍五入为它最接近的整数（int 类型） |\n| static long round(double a) | 双精度浮点数 a 四舍五入为它最接近的整数（long 类型） |', 63, 2),
(433, 42, 38, 1, '程序清单 2-12 展示了取整方法的使用。', 63, 3),
(434, 42, 38, 1, '求最大值 max、最小值 min 的方法都有两个参数。例如，Math.max(4, 6) 返回 6，Math.min(4, 6) 返回 4。这两个方法的参数可以是 int、long、float、double 型。例如，Math.max(4.5,6.5) 返回 double 型的值 6.5，Math.max(4.5f,6.5f) 返回 float 型的值 6.5。', 64, 4),
(435, 42, 38, 1, '求绝对值 abs 方法有一个参数，这个参数的类型也可以是 int、long、float、double 型。例如，Math.abs(-3) 返回 3，Math.abs(-3.3) 返回 3.3。', 64, 5),
(436, 42, 38, 1, '随机方法 random（public static double random()）生成一个大于等于 0.0 且小于 1.0 的 double 类型随机数。使用该方法编写表达式，可以生成指定范围的随机数。假设 a、b 为两个浮点数且 a≤b，生成 a~b（包含 a、不含 b）的随机数，其计算表达式为：', 64, 6),
(437, 42, 38, 1, 'a + Math.random()*(b-a)', 64, 7),
(438, 42, 38, 1, '例如，生成一个 50.5~82.3 的随机数，其计算表达式为：', 64, 8),
(439, 42, 38, 1, '50.5 + Math.random()*(82.3-50.5)', 64, 9),
(440, 42, 38, 1, '如果要生成一个范围内的随机整数，那么需要使用强制类型转换。', 64, 10),
(441, 42, 38, 1, '假设 a、b 为两个整数且 a≤b，生成 a~b（包含 a、b）的随机数，其计算表达式为：', 64, 11),
(442, 42, 38, 1, 'a + (int)(Math.random()*(b-a+1))', 64, 12),
(443, 42, 38, 1, '例如，生成一个 50~85 的随机整数，其计算表达式为：', 64, 13),
(444, 42, 38, 1, '50 + (int)(Math.random()*(85-50+1))', 64, 14),
(445, 44, 40, 1, '编程是编写程序的简称，是使用特定计算机语言编写计算机程序，以控制计算机执行特定任务或解决特定问题。在 Java 语言编程中，程序员用 Java 语言编写程序代码解决某个特定问题。', 64, 1),
(446, 44, 40, 1, '编程解决某个特定实际问题，一般需要两个步骤。第一步，对问题进行求解，设计解决问题的策略，这种求解问题的策略也被称为算法。第二步，应用某种程序设计语言把这种策略转换成可以在现代通用计算机上执行的程序。', 64, 2),
(447, 44, 40, 1, '下面通过编程求解一个简单问题来说明上述步骤。', 64, 3),
(448, 44, 40, 1, '该简单问题是：输入一个三角形三条边的边长值，计算并显示该三角形的面积。', 65, 4),
(449, 44, 40, 1, '首先，设计解决该问题的算法，算法可以用自然语言、伪码（自然语言与一些程序设计代码的混合）、程序流程图来描述。', 65, 5),
(450, 44, 40, 1, '下面以自然语言描述、程序流程图描述分别来求解该问题的算法。', 65, 6),
(451, 44, 40, 1, '自然语言描述的算法如下：', 65, 7),
(452, 44, 40, 1, '第一步，输入一个三角形的三条边 a、b、c；', 65, 8),
(453, 44, 40, 1, '第二步，计算该三角形周长的一半 p=(a+b+c)/2；', 65, 9),
(454, 44, 40, 1, '第三步，使用海伦公式计算该三角形的面积：area=√p(p-a)(p-b)(p-c)；', 65, 10),
(455, 44, 40, 1, '第四步，输出该三角形的面积。', 65, 11),
(456, 44, 40, 1, '程序流程图描述的算法如图 2-24 所示。', 65, 12),
(457, 44, 40, 1, '图 2-24　求三角形面积算法的程序流程图（图：按表 2-23 的基本符号绘制——开始 → 输入三角形的三条边 a、b、c → 计算 p=(a+b+c)/2 → 计算 area=√p(p-a)(p-b)(p-c) → 输出 area → 结束）', 65, 13),
(458, 44, 40, 1, '表 2-23 给出了程序流程图的基本符号。', 65, 14),
(459, 44, 40, 1, '表 2-23　程序流程图的基本符号\n| 符号 | 描述 |\n| --- | --- |\n| 椭圆或圆角矩形（图形） | 椭圆或圆角矩形，表示开始或结束 |\n| 平行四边形（图形） | 平行四边形，表示一个数据输入或数据输出的处理步骤 |\n| 矩形（图形） | 矩形，表示一个单独的处理步骤 |\n| 菱形（图形） | 菱形，表示一项判定，引出两个分支 |\n| 箭头（图形） | 箭头，表示一个过程的流程方向 |', 65, 15),
(460, 44, 40, 1, '上述算法需要转换成程序，本书用 Java 语言将算法转换成程序。每个 Java 程序均是以类的声明开始。根据待求解的问题，可以定义类名为 ComputeTriangleArea，于是，这个程序的框架如下所示：', 65, 16),
(461, 44, 40, 1, 'Java 程序的执行需要一个 main 方法，于是结合上述算法，程序扩展如下：', 65, 17),
(462, 44, 40, 1, '对于 step 1，可以定义三个 double 类型变量 a、b、c。然后，使用 Scanner 类输入三个 double 类型数。为了对用户友好，在输入数据之前，应给出提示输入信息。', 66, 18),
(463, 44, 40, 1, '对于 step 2，需要定义一个变量 p，并将数学公式转换成 Java 表达式：', 66, 19),
(464, 44, 40, 1, '这样就计算出三角形周长值的一半了。', 66, 20),
(465, 44, 40, 1, '对于 step 3，需要使用一个求平方根的方法 Math.sqrt()，于是计算三角形面积的 Java 表达式如下：', 66, 21),
(466, 44, 40, 1, '对于 step 4，可以使用 printf 方法或 println 方法输出三角形面积信息。例如，可使用如下语句：', 66, 22),
(467, 44, 40, 1, '该示例完整的程序如程序清单 2-13 所示。', 66, 23),
(468, 44, 40, 1, '程序清单 2-13 第 7~9 行输入了三角形的三条边，第 12 行计算三角形周长的一半，第 15 行计算三角形面积，第 18 行输出三角形面积。', 66, 24),
(469, 45, 41, 1, '通过 2.8.1 节简单问题的求解示例，可以看到算法设计的重要性。在编写代码之前，以算法形式描述问题的求解策略，有助于我们更好地思考问题和做好规划，锻炼算法思维和计算思维。', 66, 1),
(470, 45, 41, 1, '算法本身就蕴含计算思维的思想和方法，甚至有学者认为，在算法思维的培养中，其实已经培养了大部分的计算思维。因此，针对实际问题进行算法设计和编程，不仅能训练编程能力，还能有效培养算法思维和计算思维。我们在学习编程的过程中，针对特定的实际问题，应养成先设计算法再编码的良好习惯。', 67, 2),
(471, 45, 41, 1, '注意：算法思维是构建和理解与算法有关的思维模式，包括算法权衡、系统模型设计、算法策略选择、约简等。而计算思维的概念由周以真教授于 2006 年提出，该定义被国际学术界广泛采用。计算思维是运用计算机科学的基础概念进行问题求解、系统设计以及人类行为理解等涵盖计算机科学之广度的一系列思维活动。计算思维源于计算机科学，又高于计算机科学。', 67, 3),
(472, 45, 41, 1, '下面介绍算法的定义和特性。算法是特定问题求解步骤的准确而完整的描述，是解决问题的有限指令序列。其中，每条指令表示计算机的一个或多个操作。', 67, 4),
(473, 45, 41, 1, '一个算法具有以下 5 个重要的特性：', 67, 5),
(474, 45, 41, 1, '(1)有穷性：算法必须在执行有限个步骤之后结束，且每个步骤在有限时间内完成。', 67, 6),
(475, 45, 41, 1, '(2)确定性：算法的每个步骤必须有确切的定义，让算法的执行者或阅读者能明确其含义以及如何执行。', 67, 7),
(476, 45, 41, 1, '(3)可行性：算法的所有步骤均可以分解为基本的、可执行的操作。因此，算法可通过有限次的基本操作来实现其功能。', 67, 8),
(477, 45, 41, 1, '(4)有输入：一个算法有零个或多个输入，可作为算法加工的初始条件。', 67, 9),
(478, 45, 41, 1, '(5)有输出：一个算法有一个或多个输出，可反映算法进行信息加工后得到的结果。', 67, 10),
(479, 45, 41, 1, '一个设计好的算法可以有多种实现方式，可以用 Java 语言编写，也可以用 C 语言、Python 语言等编写。用某种编程语言实现的算法，就是程序。不同编程语言在实现算法步骤时，会有不同的要求。另一方面，程序并不等同于算法。例如，操作系统程序在用户没有退出、计算机未出现故障、未停电的情况下可以无限运行。而算法是有穷的，在执行完有限指令序列后会停止。', 67, 11),
(480, 46, NULL, 1, '体现软件质量的一个重要属性是程序的易读性。易读性好的程序，既能帮助程序员避免错误，也有利于对程序进行维护和修改。尤其是程序的规模变得庞大时，程序的易读性会变得更加重要。而提升程序的易读性，则需要程序员根据一些编程规范，养成良好的编程习惯。在编程时，程序员需要在代码排版时遵循一些良好的程序设计风格，在代码的恰当地方给出适当的注释，对程序元素的命名能见名知意，养成良好的命名习惯。本节介绍一些编程规范，包括良好的程序设计风格、恰当的注释和文档以及良好的程序元素命名习惯。', 67, 1),
(481, 47, 43, 1, '程序设计风格是对程序代码排版所设计出来的外观样式。对于 Java 语言，程序员把整个程序写在一行上，JVM 也能编译和运行这个程序。然而，这样的程序可读性非常差，也容易出错，不便于代码的编辑、调试与维护。因此，为了避免这种可读性差的程序，程序员在编程时，应根据人类的阅读习惯，对代码进行分行、分段、缩进，并在合适的地方给出排版良好的、恰当的注释。', 67, 1),
(482, 47, 43, 1, '程序代码应根据其功能逻辑进行分段，可由一个空行进行分段（本书的一些示例代码为了节约空间未进行分段）。如果一段代码需要注释，程序员可以用单独一行注释对这段代码进行说明。一般来说，一条语句占据一行。另外，代码应保持一致的缩进风格。在源代码编辑器中，缩进（Indentation）是代码与左边边界之间的距离，通过缩进，程序员可以展示程序中各部分的结构性关系。例如，在嵌套结构中，每个内层的组成部分应该比外层缩进至少 2 个空格，体现出内外层的关系。前面所举示例均具有良好的、一致的缩进风格。另外，二元或三元运算符在连接多个操作数时，应该在运算符两边各留一个空格，使得程序读起来更舒适，如下所示：', 68, 2),
(483, 47, 43, 1, 'int i = 3 + 4;   //好的风格', 68, 3),
(484, 47, 43, 1, 'int i=3+4;   //不好的风格，显得局促', 68, 4),
(485, 47, 43, 1, '在 Java 语言中，程序员经常会使用代码块（Block）的构造，代码块是由花括号括起来的一组语句。代码块的写法有两种常见风格，即行尾风格和次行风格，如图 2-25 所示。', 68, 5),
(486, 47, 43, 1, '图 2-25　行尾风格与次行风格（图：左右两栏代码截图对比——行尾风格将起始花括号置于一行的末位，结束花括号采用缩进方式表示结构关系；次行风格将起始花括号置于新的一行并适当缩进，与结束花括号垂直对齐）', 68, 6),
(487, 47, 43, 1, '行尾风格将起始花括号置于一行的末位，结束花括号采用缩进方式表示结构关系。次行风格将起始花括号置于新的一行，并采用适当缩进，与结束花括号垂直对齐。行尾风格可节省排版空间，次行风格更加宽松，两种风格都具有好的易读性。编程时，程序员应该统一采用一种风格，建议不要混合使用这两种风格。在一个项目组中，整个项目也应该定义好一种统一的风格，不要混合使用两种风格。本书与 Java API 源码一样，采用行尾风格。', 68, 7),
(488, 48, 44, 1, '良好的注释有助于程序员理解和维护代码。一般来讲，程序员可以在程序的开头写一个摘要，说明这个程序是做什么的、具有哪些功能和独特技术等。程序员还可以对程序中一些具有技巧和复杂性的主要步骤或语句进行注释。注释应写得简明扼要，不能喧宾夺主，不能让程序充满注释，反而影响程序的易读性。', 68, 1),
(489, 48, 44, 1, 'Java 语言提供了丰富的注释方式，包括 3 种注释，分别是行注释（Line Comment）、块注释（Block Comment）、Java 文档注释（Javadoc Comment）。行注释和块注释在 1.3.1 节中已经进行了讲述。本节主要介绍 Java 文档注释。', 68, 2),
(490, 48, 44, 1, 'Java 文档注释是用来生成应用程序接口（API）文档的。Java 文档注释以 /** 开始，并以 */ 结束，通过使用“javadoc”命令将文档提取到一个 HTML 文件中。Java 文档注释可以用来注释类、接口、方法、成员变量、构造方法和内部类等。另外，Java 文档注释只处理文档源文件在类、接口、方法、成员变量、构造方法和内部类之前的注释，忽略其他地方的文档注释，然后形成一个和源代码配套的 API 帮助文档。', 68, 3),
(491, 48, 44, 1, '下面通过一个示例说明 Java 文档注释及基于 Java 文档注释所生成的 API 帮助文档。该示例的代码如程序清单 2-14 所示。', 69, 4),
(492, 48, 44, 1, '程序清单 2-14　TestJavaDoc.java', 69, 5),
(493, 48, 44, 1, '命令行下执行 javadoc 命令如下：', 69, 6),
(494, 48, 44, 1, '本示例在使用 javadoc 命令时，使用 -d 选项，指明了生成的 API 文档所放置的目标目录。本示例在 Windows 下执行，指定生成文档放置在目录 e:\\temp\\doc\\ 中。接着，在指定生成文档的存放目录之后，该命令指定将要处理的 Java 源文件名为“TestJavaDoc.java”。在 javadoc 命令执行完后，目录 e:\\temp\\doc\\ 下生成了 22 个文档，如图 2-26 所示。用户可以打开图 2-26 所示的“index.html”文件，在浏览器中查看针对 TestJavaDoc.java 生成的帮助文档。', 69, 7),
(495, 48, 44, 1, '图 2-26　javadoc 生成的文档（图：e:\\temp\\doc\\ 目录下生成的 22 个文档截图，包括 index.html、TestJavaDoc.html、package-summary.html、package-tree.html、allclasses-index.html、allpackages-index.html、overview-tree.html、index-all.html、help-doc.html 以及 script.js、search.js、stylesheet.css、各类 *-search-index.js 等资源文件）', 69, 8),
(496, 48, 44, 1, '程序清单 2-14 第 1~4 行是对整个类的注释，也是对整个程序的描述。通常，建议使用标签 @author 说明作者。第 6~9 行是对 main 方法的说明，通常建议使用标签 @param 对方法的每个参数进行说明。', 70, 9),
(497, 48, 44, 1, '标签以 @ 开头，并且区分大小写，它们必须使用大写和小写字母输入。标签必须在行首开始，否则将被视为普通文本。', 70, 10),
(498, 48, 44, 1, 'javadoc 命令在生成 Java 文档注释时，可以解析特殊标签，如 @author、@param。这些标签可以使程序员能够从源代码自动生成完整的、格式良好的 API 说明。例如，针对 @param 标签，在生成的文档中，会有如图 2-27 所示的说明。', 70, 11),
(499, 48, 44, 1, '图 2-27　生成的帮助文档中的参数说明（图：API 帮助文档页面截图，“参数”部分显示“args—命令行参数”）', 70, 12),
(500, 48, 44, 1, '生成的帮助文档在“参数”部分，说明了“args—命令行参数”，与注释时的信息一致。', 70, 13),
(501, 48, 44, 1, 'javadoc 命令的一般形式如下：', 70, 14),
(502, 48, 44, 1, 'javadoc [options] [packagenames] [sourcefiles] [@files]', 70, 15),
(503, 48, 44, 1, '其中，各部分含义如下：', 70, 16),
(504, 48, 44, 1, '(1) options 表示 javadoc 命令选项，可以通过命令 javadoc -help 进行查看。', 70, 17),
(505, 48, 44, 1, '(2) packagenames 表示包名。一系列包的名称，以空格分隔，如 java.lang java.lang.reflect java.awt 等，必须单独指定要记录的每个包，不允许使用通配符。', 70, 18),
(506, 48, 44, 1, '(3) sourcefiles 表示源文件名。', 70, 19),
(507, 48, 44, 1, '(4) @files 表示从文件中读取选项和文件名。', 70, 20),
(508, 48, 44, 1, '在命令提示符窗口执行命令 javadoc -help | more，查看 javadoc 的用法和选项，如图 2-28 所示（其内容较长，仅显示一个页面）。', 70, 21),
(509, 48, 44, 1, '图 2-28　javadoc -help 命令的用法和选项（图：命令提示符窗口截图，显示 javadoc 命令帮助信息的第一页，包含用法“javadoc [options] [packagenames] [sourcefiles] [@files]”及各选项说明）', 70, 22),
(510, 48, 44, 1, 'Java 的 IDE（Eclipse、IntelliJ IDEA）也有相应的生成 API 文档的功能，篇幅所限，这里仅介绍 Eclipse 环境下生成的 Java 文档。为了便于演示，首先创建一个项目 TestJavaDoc，然后创建一个包 test1，接着创建一个类 Main1。Main1 的代码如下所示：', 71, 23),
(511, 48, 44, 1, '在 Eclipse 中，选择菜单“Project”，在打开的菜单中单击菜单项“Generate Javadoc...”，这时 Eclipse 会为项目 TestJavaDoc 生成文档。Eclipse 在生成文档注释时，控制台中会出现如下信息：', 71, 24),
(512, 48, 44, 1, '然后，在 Package Explorer 窗口中可以看到在项目 TestJavaDoc 下生成了一个 doc 文件夹，如图 2-29(a) 所示。单击打开 doc 文件夹中的文件“index.html”，会出现如图 2-29(b) 所示的网页；接着单击该网页上的“程序包”选项，会出现如图 2-29(c) 所示的网页；再继续单击这个网页上的“类”→“Main1”选项，会出现如图 2-29(d) 所示的网页。在图 2-29(d) 所示的网页上，可以看到在程序中的注释信息。图 2-29(e) [图 2-29(d) 网页的一部分] 可以看到针对公共字段（静态常量）的注释，其他类型字段无需进行文档注释。图 2-29(f) [图 2-29(d) 网页的一部分] 可以看到针对整个类的注释，有作者信息 Liu 和版本信息 1.0，对应注释中的 @author Liu、@version 1.0。图 2-29(g) [图 2-29(d) 的网页的一部分] 可以看到针对方法的注释信息，说明了参数信息和返回值信息。其中，标签 @param 针对每个参数都可以进行说明，描述文本可以跨越多行。如果使用了文档注释而没有对方法的某个参数使用标签 @param 进行注释，就会产生警告错误。而且，一个方法的所有 @param 标签都要放在一起。一个方法的 @return 标签用于给当前方法增加返回值信息说明，描述文本也可以跨越多行。', 72, 25),
(513, 48, 44, 1, '图 2-29　Eclipse 生成 Java 文档过程（图：(a) Package Explorer 中项目 TestJavaDoc 下生成的 doc 文件夹；(b) 打开 index.html 显示的帮助文档首页；(c) 单击“程序包”后显示的页面；(d) 单击“类”→“Main1”后显示的页面；(e) 针对公共字段（静态常量）DISC 的注释；(f) 针对整个类的注释，含作者信息 Liu 和版本信息 1.0；(g) 针对方法的注释信息，说明参数和返回值）', 72, 26),
(514, 49, 45, 1, '程序员在编程时，常常要为变量、常量、方法、类、接口等程序元素命名。命名的好坏会极大影响代码的阅读体验。', 73, 1),
(515, 49, 45, 1, '如果一个标识符包含多个英文单词，可以采用两种方式处理。', 73, 2),
(516, 49, 45, 1, '一是驼峰命名法，即把所有的单词连接在一起，第一个单词的首字母采用小写或大写，而后面的每个单词的首字母采用大写。', 73, 3),
(517, 49, 45, 1, '二是下划线命名法，即第一个单词的首字母采用小写或大写，而后面的每个单词均采用小写且用一个下划线连接。当命名变量和方法时，标识符的第一个单词首字母应小写。', 73, 4),
(518, 49, 45, 1, '当命名类、接口时，标识符的第一个单词的首字母应大写。例如，使用驼峰命名法时，一个方法名可定义为 getArea，一个接口名可定义为 CompareArea。在使用下划线命名法时，方法名可定义为 get_area，接口名可定义为 Compare_area。', 73, 5),
(519, 49, 45, 1, '如果标识符定义一个常量，那么标识符的所有字母应全部大写。例如，圆周率是一个常数，可用标识符 PI 表示。', 74, 6),
(520, 49, 45, 1, '另外，Java 语言也支持利用中文来命名标识符。但是，中文输入相比英文字母输入麻烦，因此中文命名标识符应少用、慎用。', 74, 7),
(521, 49, 45, 1, '在一份源代码中，应采用统一命名方式对源代码中的标识符进行命名。最好不要在一份源代码中一会儿采用驼峰命名法，一会儿采用下划线命名法。', 74, 8),
(522, 50, NULL, 1, '程序设计错误可以分为 3 类：编译错误、运行时错误和逻辑错误。', 74, 1),
(523, 51, 47, 1, '编译错误（Compile Error）是指在编译过程中由编译器检测到的错误，一般是编译器检测到的语法错误（Syntax Error）。因此，编译错误也被称为语法错误。例如，一些常见的编译错误有：输入关键字错误、左括号没有与之匹配的右括号错误、遗漏标点符号错误、一个变量没有赋值就直接使用错误等。编译器可以轻松地检测到这些错误，并报告错误在什么地方以及是什么原因导致的。例如，程序清单 2-15 所示的程序在编译时，会出现编译错误。', 74, 1),
(524, 51, 47, 1, '程序清单 2-15　ShowCompileError.java', 74, 2),
(525, 51, 47, 1, 'Eclipse 环境下的编译错误提示如图 2-30 所示。', 74, 3),
(526, 51, 47, 1, '图 2-30　Eclipse 环境下的编译错误提示（图：Eclipse Problems 面板截图——Description：Errors(1 item)，The local variable i may not have been initialized；Resource：ShowCompileError.java；Path：/JavaSrc/src/chapt2；Location：line 5；Type：Java Problem）', 74, 4),
(527, 51, 47, 1, '该提示指明了错误发生的地方是第 5 行，原因是局部变量 i 没有被初始化。', 74, 5),
(528, 51, 47, 1, '一个程序也有可能存在多个编译错误。在出现编译错误时，程序员应根据提示逐一仔细检查源代码，审查出现错误的地方：有无违反程序设计语言的语法规则，有无遗漏字符或标点符号等。只要对 Java 语言的语法掌握良好，则程序员根据 Java 编译器给出的编译错误提示信息，纠正编译错误就较为容易。', 74, 6),
(529, 52, 48, 1, '一个程序在编译通过之后，说明该程序没有编译错误了。然而，一个程序在运行时，也有可能出现错误，导致程序异常终止，这就是运行时错误（Runtime Error）。一个典型的运行时错误就是将 0 用作除数的情况。例如，程序清单 2-16 所示的程序在编译时不会出现问题，可以运行。在运行时，如果除数输入的数值是零，就会出现运行时错误。', 75, 1),
(530, 52, 48, 1, '程序清单 2-16　ShowRuntimeError.java', 75, 2),
(531, 52, 48, 1, '当除数不为零时，程序可以正常运行，运行结果如下：', 75, 3),
(532, 52, 48, 1, '当除数为零时，程序会异常终止，运行结果如下：', 75, 4),
(533, 52, 48, 1, '程序清单 2-16 第 12 行是一个除法运算，当除数不为零时，可以正常运行，没有任何错误。然而，当除数为零时，就会抛出运行时异常 ArithmeticException，并导致程序终止，运行结果也会显示产生运行时错误的原因和位置。因此，运行时错误可以通过运行测试程序来发现，并且也容易被修正。', 75, 5),
(534, 53, 49, 1, '逻辑错误（Logic Error）是指程序没有正确达到预期的功能。这种错误发生的原因有很多种，有些是错误的编码引起的，有些是对需求的理解发生偏差引起的。例如，程序清单 2-9 中的程序是将摄氏温度转换为华氏温度，如果编写代码时忽略了整数除法运算的特殊性，就会引起逻辑错误。程序清单 2-17 所示的程序演示了编码引起的逻辑错误。', 75, 1),
(535, 53, 49, 1, '程序清单 2-17　ShowLogicError.java', 76, 2),
(536, 53, 49, 1, '运行结果如下：', 76, 3),
(537, 53, 49, 1, '这个运行结果没有达到预期功能，36.5℃ 对应的华氏温度应该是 97.7℉。产生这个错误的原因是 (9/5) 在数学上可以得到 1.8，但在 Java 语言中 (9/5) 的运算结果是 1，这样就出现了转换错误，未能达到预期功能，因此，就出现了逻辑错误。一般来说，逻辑错误比较隐秘，难以发现，需要通过相应的测试技术才能发现。逻辑错误对程序的影响可大可小。对于程序清单 2-17 所示的例子，逻辑错误影响不大，通过测试发现错误后，可以容易地解决，只要将第 9 行的 (9/5) 修改成 (9.0/5) 即可。然而，逻辑错误有可能影响很大。例如，当程序员对某个需求的理解发生了偏离，通过测试发现编写的程序没能完成预期功能时，有可能需要对整个程序的代码进行重新编写，而不仅仅是修改几行代码。', 76, 4),
(538, 14, 50, 1, '基础习题2　编程习题2', 76, 1);

INSERT INTO `knowledge_chapter` (`id`, `course_id`, `parent_id`, `chapter_code`, `title`, `level`, `sort_order`, `description`, `summary`) VALUES
(14, 1, 0, '2', '第 2 章 Java 编程基础', 1, 2, '本章介绍 Java 编程的基础知识，包括标识符与关键字、变量与常量、赋值语句与赋值表达式、命令行交互工具 JShell、基本数据类型、运算符和表达式求值、数学函数、编程与算法、编程规范以及程序设计错误，是学习 Java 语言程序设计的重要基础。', '本章内容是学习 Java 语言程序设计的重要基础，包含计算机编程语言中一些常见的编程概念和编程基础知识。本章不仅介绍了 ASCII 码、Unicode 码、标识符、变量、常量、赋值语句、基本数据类型及数据类型转换、运算符及混合运算、控制台输入/输出等基本编程元素，而且介绍了一种新的交互式工具 JShell，方便用户运行和调试代码片段。进一步，介绍了数学函数及其使用以及如何通过编程解决实际问题。针对特定实际问题进行编程求解时，读者应养成先设计算法的良好习惯。算法设计可有效培养读者的算法思维和计算思维。最后，本章还介绍了易读性好的代码应遵循的编程规范以及在编程过程中碰到的 3 类错误。'),
(15, 1, 14, '2.1', '标识符与关键字', 2, 1, '人类能阅读和理解的字符在计算机中是以 0 和 1 构成的二进制序列来存储的。把字符映射到其二进制序列的过程称为编码（Encoding）。编码是信息从一种形式或格式转换为另一种形式的过程。解码（Decoding）是编码的逆过程。在计算机发展过程中，出现了多种编码方案，如 ASCII 码、Unicode 码、GBK 码（GBK 码可对 2 万多的简繁汉字进行编码）、ISO-8859-1 码（ISO-8859-1 码是单字节编码，向下兼容 ASCII，其编码范围是 0x00~0xFF，0x00~0x7F 之间完全和 ASCII 一致，0x80~0x9F 之间是控制字符，0xA0~0xFF 之间是文字符号）等。早期编程语言如 C、C++ 等都是使用 ASCII 码的。每一种编码方案都定义了如何编码每个字符。Java 语言使用的编码方案是 Unicode 码。', NULL),
(16, 1, 15, '2.1.1', 'ASCII 码和 Unicode 码', 3, 1, NULL, NULL),
(17, 1, 15, '2.1.2', '标识符', 3, 2, NULL, NULL),
(18, 1, 15, '2.1.3', '关键字', 3, 3, NULL, NULL),
(19, 1, 14, '2.2', '变量与常量', 2, 2, NULL, NULL),
(20, 1, 19, '2.2.1', '变量', 3, 1, NULL, NULL),
(21, 1, 19, '2.2.2', '常量', 3, 2, NULL, NULL),
(22, 1, 14, '2.3', '赋值语句与赋值表达式', 2, 3, '变量（Variable）在声明之后，可以使用赋值语句（Assignment Statement）对其赋值（赋初值或修改其值）。赋值语句使用赋值运算符“=”（等号），赋值语句的语法如下所示：', NULL),
(23, 1, 14, '2.4', '命令行交互工具 JShell', 2, 4, 'JShell（Java Shell）工具是自 Java SE 9 开始引入的一个新特性——命令行交互工具，以 REPL（Read-Eval-Print Loop，读取——计算——打印循环）方式交互式评估 Java 语言的声明、语句和表达式。JShell 支持单个表达式或语句的执行，并立即查看结果，不需要编写完整的类。', NULL),
(24, 1, 14, '2.5', '基本数据类型', 2, 5, 'Java 语言的数据类型分为基本数据类型和引用数据类型两大类，如图 2-13 所示。基本数据类型又可分为数值类型和布尔类型。数值类型又可分为整数类型和浮点类型。整数类型包括 4 种有符号整数类型（byte、short、int、long）和一种无符号整数类型（char，又称字符类型）。浮点类型又可分为单精度浮点型（float）和双精度浮点型（double）。因此，Java 语言的基本数据类型有 7 种，每种基本数据类型数据的取值范围和占用的存储空间都是固定的，不依赖于具体的计算机。（图 2-13　Java 数据类型的分类：基本数据类型分为数值类型和布尔类型，数值类型分为整数类型和浮点类型，引用数据类型另行详述）', NULL),
(25, 1, 24, '2.5.1', '整数类型', 3, 1, NULL, NULL),
(26, 1, 24, '2.5.2', '浮点类型', 3, 2, NULL, NULL),
(27, 1, 24, '2.5.3', '字符类型', 3, 3, NULL, NULL),
(28, 1, 24, '2.5.4', '布尔类型', 3, 4, NULL, NULL),
(29, 1, 24, '2.5.5', '数据类型转换', 3, 5, NULL, NULL),
(30, 1, 24, '2.5.6', '输入基本数据类型', 3, 6, NULL, NULL),
(31, 1, 24, '2.5.7', '格式化控制台输出', 3, 7, NULL, NULL),
(32, 1, 14, '2.6', '运算符和表达式求值', 2, 6, 'Java 语言提供了丰富的运算符，包括算术运算符、关系运算符、逻辑运算符、赋值与增强赋值运算符、位运算符等。Java 语言使用的运算符与 C 语言、C++ 语言基本相同。', NULL),
(33, 1, 32, '2.6.1', '算术运算符', 3, 1, NULL, NULL),
(34, 1, 32, '2.6.2', '增强赋值运算符', 3, 2, NULL, NULL),
(35, 1, 32, '2.6.3', '关系运算符', 3, 3, NULL, NULL),
(36, 1, 32, '2.6.4', '逻辑运算符', 3, 4, NULL, NULL),
(37, 1, 32, '2.6.5', '条件运算符', 3, 5, NULL, NULL),
(38, 1, 32, '2.6.6', '运算符总述', 3, 6, NULL, NULL),
(39, 1, 14, '2.7', '数学函数', 2, 7, 'Java 语言在 java.lang 包（Java 语言基础包，隐式导入到程序中）中提供了 Math 类，在程序中可以直接使用 Math 类。Math 类提供许多常用的数学函数方法，这些方法可以分为 3 类：三角函数方法（Trigonometric Method）、指数函数方法（Exponent Method）和服务方法（Service Method）。除了这些方法，Math 类还提供了两个很有用的 double 型常量：PI（圆周率 π，约等于 3.1415926）和 E（自然对数的底，约等于 2.71828）。', NULL),
(40, 1, 39, '2.7.1', '三角函数', 3, 1, NULL, NULL),
(41, 1, 39, '2.7.2', '指数函数', 3, 2, NULL, NULL),
(42, 1, 39, '2.7.3', '服务方法', 3, 3, NULL, NULL),
(43, 1, 14, '2.8', '编程与算法', 2, 8, NULL, NULL),
(44, 1, 43, '2.8.1', '编程', 3, 1, NULL, NULL),
(45, 1, 43, '2.8.2', '算法', 3, 2, NULL, NULL),
(46, 1, 14, '2.9', '编程规范', 2, 9, '体现软件质量的一个重要属性是程序的易读性。易读性好的程序，既能帮助程序员避免错误，也有利于对程序进行维护和修改。尤其是程序的规模变得庞大时，程序的易读性会变得更加重要。而提升程序的易读性，则需要程序员根据一些编程规范，养成良好的编程习惯。在编程时，程序员需要在代码排版时遵循一些良好的程序设计风格，在代码的恰当地方给出适当的注释，对程序元素的命名能见名知意，养成良好的命名习惯。本节介绍一些编程规范，包括良好的程序设计风格、恰当的注释和文档以及良好的程序元素命名习惯。', NULL),
(47, 1, 46, '2.9.1', '程序设计风格', 3, 1, NULL, NULL),
(48, 1, 46, '2.9.2', '注释', 3, 2, NULL, NULL),
(49, 1, 46, '2.9.3', '命名习惯', 3, 3, NULL, NULL),
(50, 1, 14, '2.10', '程序设计错误', 2, 10, '程序设计错误可以分为 3 类：编译错误、运行时错误和逻辑错误。', NULL),
(51, 1, 50, '2.10.1', '编译错误', 3, 1, NULL, NULL),
(52, 1, 50, '2.10.2', '运行时错误', 3, 2, NULL, NULL),
(53, 1, 50, '2.10.3', '逻辑错误', 3, 3, NULL, NULL);

INSERT INTO `knowledge_point` (`id`, `chapter_id`, `name`, `description`, `difficulty`, `keyword`, `page_ref`, `page_order`) VALUES
(13, 14, '教学目标', NULL, 1, '教学目标', 26, 1),
(14, 16, 'ASCII 码和 Unicode 码', NULL, 1, '编码,ASCII 码,Unicode 码,美标', 27, 1),
(15, 17, '标识符', NULL, 1, '标识符,Java 字母,合法标识符', 'P.28-29', 1),
(16, 18, '关键字', NULL, 1, '关键字,保留关键字,上下文关键字', 29, 1),
(17, 20, '变量', NULL, 1, '变量,变量声明,初始化', 'P.29-30', 1),
(18, 21, '常量', NULL, 1, '常量,final,字面值', 31, 1),
(19, 22, '赋值语句与赋值表达式', NULL, 1, '赋值语句,赋值表达式,链式赋值', 'P.31-32', 1),
(20, 23, '命令行交互工具 JShell', NULL, 1, 'JShell,代码段,REPL,命令', 'P.32-36', 1),
(21, 25, '整数类型', NULL, 1, '整数类型,整型字面值,进制', 'P.37-38', 1),
(22, 26, '浮点类型', NULL, 1, '浮点类型,浮点型字面值,IEEE 754', 'P.38-40', 1),
(23, 27, '字符类型', NULL, 1, '字符类型,字符字面值,转义序列', 'P.40-42', 1),
(24, 28, '布尔类型', NULL, 1, '布尔类型,boolean,true,false', 42, 1),
(25, 29, '数据类型转换', NULL, 1, '数据类型转换,隐式类型转换,强制类型转换', 'P.42-43', 1),
(26, 30, '输入基本数据类型', NULL, 1, 'Scanner,输入,hasNext', 'P.44-48', 1),
(27, 31, '格式化控制台输出', NULL, 1, '格式化输出,printf,格式标识符', 'P.48-51', 1),
(29, 33, '算术运算符', NULL, 1, '算术运算符,取余运算符,优先级', 'P.52-56', 1),
(30, 34, '增强赋值运算符', NULL, 1, '增强赋值运算符,+=,自动类型转换', 'P.56-57', 1),
(31, 35, '关系运算符', NULL, 1, '关系运算符,比较运算符,布尔值', 57, 1),
(32, 36, '逻辑运算符', NULL, 1, '逻辑运算符,真值表,短路运算', 'P.57-58', 1),
(33, 37, '条件运算符', NULL, 1, '条件运算符,三元运算符', 'P.58-59', 1),
(34, 38, '运算符总述', NULL, 1, '运算符优先级,结合方向,混合运算', 59, 1),
(36, 40, '三角函数', NULL, 1, '三角函数,弧度,Math 类', 'P.60-61', 1),
(37, 41, '指数函数', NULL, 1, '指数函数,Math.exp,Math.pow', 62, 1),
(38, 42, '服务方法', NULL, 1, '服务方法,取整,随机数', 'P.63-64', 1),
(40, 44, '编程', NULL, 1, '编程,程序设计,问题求解', 'P.64-66', 1),
(41, 45, '算法', NULL, 1, '算法,程序流程图,计算思维', 'P.66-67', 1),
(43, 47, '程序设计风格', NULL, 1, '程序设计风格,缩进,代码块', 'P.67-68', 1),
(44, 48, '注释', NULL, 1, '注释,文档注释,javadoc', 'P.68-73', 1),
(45, 49, '命名习惯', NULL, 1, '命名习惯,驼峰命名法,下划线命名法', 'P.73-74', 1),
(47, 51, '编译错误', NULL, 1, '编译错误,语法错误', 'P.74-75', 1),
(48, 52, '运行时错误', NULL, 1, '运行时错误,异常', 75, 1),
(49, 53, '逻辑错误', NULL, 1, '逻辑错误,程序调试', 'P.75-76', 1),
(50, 14, '习题', NULL, 1, '习题', 76, 1);

INSERT INTO `knowledge_weight_config` (`id`, `chapter_id`, `knowledge_point_id`, `weight`, `base_question_count`, `max_question_count`, `difficulty_distribution`, `is_active`) VALUES
(13, 14, 13, 1.00, 1, 5, '30,50,20', 1),
(14, 16, 14, 1.00, 1, 5, '30,50,20', 1),
(15, 17, 15, 1.00, 1, 5, '30,50,20', 1),
(16, 18, 16, 1.00, 1, 5, '30,50,20', 1),
(17, 20, 17, 1.00, 1, 5, '30,50,20', 1),
(18, 21, 18, 1.00, 1, 5, '30,50,20', 1),
(19, 22, 19, 1.00, 1, 5, '30,50,20', 1),
(20, 23, 20, 1.00, 1, 5, '30,50,20', 1),
(21, 25, 21, 1.00, 1, 5, '30,50,20', 1),
(22, 26, 22, 1.00, 1, 5, '30,50,20', 1),
(23, 27, 23, 1.00, 1, 5, '30,50,20', 1),
(24, 28, 24, 1.00, 1, 5, '30,50,20', 1),
(25, 29, 25, 1.00, 1, 5, '30,50,20', 1),
(26, 30, 26, 1.00, 1, 5, '30,50,20', 1),
(27, 31, 27, 1.00, 1, 5, '30,50,20', 1),
(29, 33, 29, 1.00, 1, 5, '30,50,20', 1),
(30, 34, 30, 1.00, 1, 5, '30,50,20', 1),
(31, 35, 31, 1.00, 1, 5, '30,50,20', 1),
(32, 36, 32, 1.00, 1, 5, '30,50,20', 1),
(33, 37, 33, 1.00, 1, 5, '30,50,20', 1),
(34, 38, 34, 1.00, 1, 5, '30,50,20', 1),
(36, 40, 36, 1.00, 1, 5, '30,50,20', 1),
(37, 41, 37, 1.00, 1, 5, '30,50,20', 1),
(38, 42, 38, 1.00, 1, 5, '30,50,20', 1),
(40, 44, 40, 1.00, 1, 5, '30,50,20', 1),
(41, 45, 41, 1.00, 1, 5, '30,50,20', 1),
(43, 47, 43, 1.00, 1, 5, '30,50,20', 1),
(44, 48, 44, 1.00, 1, 5, '30,50,20', 1),
(45, 49, 45, 1.00, 1, 5, '30,50,20', 1),
(47, 51, 47, 1.00, 1, 5, '30,50,20', 1),
(48, 52, 48, 1.00, 1, 5, '30,50,20', 1),
(49, 53, 49, 1.00, 1, 5, '30,50,20', 1),
(50, 14, 50, 1.00, 1, 5, '30,50,20', 1);

INSERT INTO `knowledge_keyword` (`id`, `chapter_id`, `knowledge_point_id`, `keyword`, `description`, `page`, `page_order`) VALUES
(15, 14, 13, '教学目标', '本章需要掌握的知识与能力要求。', 26, 1),
(16, 16, 14, '编码（Encoding）', '把字符映射到其二进制序列的过程；解码（Decoding）是其逆过程。', 26, 1),
(17, 16, 14, 'ASCII 码', '基于拉丁字母的一套计算机编码系统，占用一个字节，用 0~127 的 128 个数字代表信息，也称"美标"。', 27, 2),
(18, 16, 14, 'Unicode 码', '占用两个字节的编码方案，能表示 65536 个字符，通过补充字符集可支持更多字符；Java 语言使用的编码方案。', 27, 3),
(19, 17, 15, '标识符', 'Java 字母和 Java 数字组成的、以 Java 字母开头的长度无限制的字符序列，用于命名程序中的类、方法、变量等元素。', 28, 1),
(20, 17, 15, 'Java 字母', '可以从整个 Unicode 字符集中提取的字符，包括大小写拉丁字母、汉字、希腊字母以及美元符号和下画线等。', 28, 2),
(21, 18, 16, '保留关键字（Reserved Keyword）', '由 ASCII 字符组成的 51 个字符序列，不能用作标识符。', 29, 1),
(22, 18, 16, '上下文关键字（Contextual Keyword）', '由 ASCII 字符组成的 16 个字符序列，可被解释为关键字或标识符，取决于出现的上下文。', 29, 2),
(23, 20, 17, '变量（Variable）', '在程序中可以被改变的量，具有变量名、数据类型、存储单元和变量值 4 个基本要素。', 29, 1),
(24, 20, 17, '变量声明', '告知 Java 编译器变量的变量名和数据类型，编译器为其分配合适的内存空间。', 30, 2),
(25, 21, 18, '常量', '在程序执行过程中不会发生改变的数据，由 final 修饰的一个合法标识符。', 31, 1),
(27, 22, 19, '赋值语句（Assignment Statement）', '使用赋值运算符“=”对变量赋值（赋初值或修改其值）的语句。', 31, 1),
(28, 22, 19, '赋值表达式（Assignment Expression）', '赋值语句去掉分号后的形式，求值结果是赋值运算符左侧变量的值。', 32, 2),
(29, 22, 19, '链式赋值', '用一个值给多个同类型变量赋值的赋值方式。', 32, 3),
(30, 23, 20, 'JShell', '自 Java SE 9 开始引入的命令行交互工具，以 REPL 方式交互式评估声明、语句和表达式。', 32, 1),
(31, 23, 20, '代码段（Snippet）', 'JShell 可接受的 Java 语句、变量定义、方法定义、类定义、导入语句和表达式等输入代码。', 32, 2),
(32, 23, 20, 'REPL', 'Read-Eval-Print Loop，读取——计算——打印循环。', 32, 3),
(33, 25, 21, '整数类型', 'Java 的 5 种整数类型：byte、short、int、long 和 char（无符号）。', 37, 1),
(34, 25, 21, '整型字面值', '可以赋值给一个整数类型的常量值，默认为 int 型，长整型需加 L 或 l 后缀。', 37, 2),
(35, 26, 22, '浮点类型', 'float（单精度浮点型）和 double（双精度浮点型）两种类型。', 38, 1),
(36, 26, 22, '浮点型字面值', '带小数点的字面值，默认为 double 型，float 型需加 f 或 F 后缀。', 38, 2),
(37, 26, 22, 'IEEE 754', '用于在计算机上表示浮点数的标准。', 38, 3),
(38, 27, 23, '字符类型（char）', '表示单个字符、占用 2 个字节的无符号整数类型，对应字符的 Unicode 编码。', 40, 1),
(39, 27, 23, '转义序列', '以反斜杠开头的字符常量表示方式。', 40, 2),
(40, 28, 24, '布尔类型（boolean）', '只有 true 和 false 两个值的类型，用于逻辑判断。', 42, 1),
(41, 29, 25, '隐式类型转换', '自动进行的类型转换，通常发生在取值范围小的类型向取值范围大的类型赋值时。', 42, 1),
(42, 29, 25, '强制类型转换', '使用 (type) 形式显式进行的类型转换。', 43, 2),
(43, 30, 26, 'Scanner', '用于获取控制台输入的类，需通过 import 语句导入 java.util 包。', 44, 1),
(44, 30, 26, '输入方法', '如 nextByte()、nextInt()、nextLong()、nextDouble()、next()、nextLine() 等，用于读取不同类型的输入。', 44, 2),
(45, 31, 27, 'printf 方法', '格式化控制台输出方法，使用格式标识符指定输出格式。', 48, 1),
(46, 31, 27, '格式标识符', '如 %d、%f、%s、%b、%c 等，占位并指定对应参数的输出格式。', 48, 2),
(47, 33, 29, '算术运算符', '包括 +、-、*、/、% 五种二元运算符和一元 +、- 运算符。', 52, 1),
(48, 33, 29, '取余运算符（%）', '取得除法运算的余数，操作数可以为浮点数。', 52, 2),
(49, 34, 30, '增强赋值运算符', '+、-、*、/、% 与赋值运算符结合形成的运算符（+=、-=、*=、/=、%=）。', 56, 1),
(50, 35, 31, '关系运算符', '也称为比较运算符，用于比较两个值的关系，运算结果为布尔值。', 57, 1),
(51, 36, 32, '逻辑运算符', '也称为布尔运算符，有 !、&&、||、^ 四种。', 57, 1),
(52, 36, 32, '真值表', '描述逻辑运算符对操作数组合的运算结果的表格。', 57, 2),
(53, 36, 32, '短路运算符', '&& 和 || 的别称，当第一个操作数已能确定结果时不再计算第二个操作数。', 58, 3),
(54, 37, 33, '条件运算符', '三元运算符“? :”，根据布尔表达式的值选择两个表达式之一的值作为结果。', 58, 1),
(55, 38, 34, '运算符优先级', '定义运算符计算先后次序的规则，圆括号优先级最高。', 59, 1),
(56, 38, 34, '结合方向', '优先级相同的运算符在一起运算时确定计算次序的规则，除赋值运算符外二元运算符均为左结合。', 59, 2),
(57, 40, 36, '三角函数方法', 'Math 类中的 sin、cos、tan 等方法，参数为以弧度表示的角度。', 60, 1),
(58, 40, 36, '弧度', '角度的另一种度量，Math.toRadians() 可将角度转换为弧度。', 60, 2),
(59, 41, 37, '指数函数方法', 'Math 类中的 exp、log、log10、sqrt、pow 方法。', 62, 1),
(60, 42, 38, '取整方法', 'Math 类的 ceil、floor、rint、round 方法。', 63, 1),
(61, 42, 38, 'random 方法', '生成大于等于 0.0 且小于 1.0 的 double 型随机数。', 64, 2),
(62, 44, 40, '编程', '使用特定计算机语言编写计算机程序，以控制计算机执行特定任务或解决特定问题。', 64, 1),
(63, 44, 40, '程序流程图', '用规定图形符号描述算法的图示工具。', 65, 2),
(64, 45, 41, '算法', '对问题进行求解的策略，可用自然语言、伪码、程序流程图描述。', 66, 1),
(65, 45, 41, '计算思维', '运用计算机科学的基础概念进行问题求解、系统设计以及人类行为理解等涵盖计算机科学之广度的一系列思维活动。', 67, 2),
(66, 47, 43, '程序设计风格', '对程序代码排版所设计出来的外观样式。', 67, 3),
(67, 47, 43, '缩进（Indentation）', '代码与左边边界之间的距离。', 68, 4),
(68, 47, 43, '行尾风格', '起始花括号置于一行的末位的代码块风格。', 68, 5),
(69, 47, 43, '次行风格', '起始花括号置于新的一行的代码块风格。', 68, 6),
(70, 48, 44, '注释', '对代码的解释说明，编译器忽略注释内容。', 68, 1),
(71, 48, 44, '文档注释（Javadoc Comment）', '以 /** 开始、以 */ 结束的注释，可用 javadoc 命令提取生成 API 文档。', 68, 2),
(72, 48, 44, 'javadoc 命令', '将文档注释提取到 HTML 文件的命令。', 69, 3),
(73, 49, 45, '命名习惯', '对程序元素命名的良好习惯，使命名见名知意。', 73, 1),
(74, 49, 45, '驼峰命名法', '变量名、方法名首单词小写其余单词首字母大写（小驼峰），类名每个单词首字母大写（大驼峰）。', 73, 2),
(75, 49, 45, '下划线命名法', '单词均小写并用下划线连接的命名方式，常用于常量命名。', 73, 3),
(76, 51, 47, '编译错误', '编译过程中由编译器检测到的错误，一般是语法错误。', 74, 1),
(77, 52, 48, '运行时错误', '程序运行时出现的导致程序异常终止的错误，如除数为零。', 75, 1),
(78, 52, 48, '异常（Exception）', '程序运行时发生的错误事件，如 ArithmeticException: / by zero。', 75, 2),
(79, 53, 49, '逻辑错误', '程序没有正确达到预期功能的错误，编译器和运行时系统都不会报告。', 75, 1),
(80, 14, 50, '习题', '本章课后基础习题与编程习题（题目正文见教材）。', 76, 1);

-- 说明：第 2 章插图（图 2-1~图 2-27）已按"（图 X-X ……）"格式并入 knowledge_content 对应正文行；程序代码块及运行结果已入 knowledge_code_snippet；表格（表 2-1~表 2-23）已按字面 \n 分隔并入 knowledge_content 对应正文行。
-- 说明：knowledge_content 的 page_order 按页连续计数（同页跨小节、跨挂点延续）；knowledge_code_snippet 的 page_order 全章连续（1~99）；knowledge_concept 的 page_order 按页连续计数，均与第 1 章约定一致。


-- WARNING: knowledge_difficult_point 未生成任何数据：源文档中无明确的"难点"标注或难点说明内容，为避免编造内容，本表留空。如需补充请依据教学大纲人工提供。
-- WARNING: knowledge_exercise 未生成任何数据：源文档"习题"部分仅包含"基础习题2　编程习题2"标题（P.76），题目正文未包含在文档正文中，无法提取题目内容，为避免编造内容，本表留空。如需生成，请参照 第1章Java语言概述.sql 的做法依据用户提供的习题正文补充。
-- 说明：第 2 章插图（图 2-1~图 2-27）已按"（图 X-X ……）"格式并入 knowledge_content 对应正文行；程序代码块及运行结果已入 knowledge_code_snippet；表格（表 2-1~表 2-23）已按字面 \n 分隔并入 knowledge_content 对应正文行。
-- 说明：knowledge_content 的 page_order 按页连续计数（同页跨小节、跨挂点延续）；knowledge_code_snippet 的 page_order 全章连续（1~99）；knowledge_concept 的 page_order 按页连续计数，均与第 1 章约定一致。




-- ============================================================
-- 第2章 Java编程基础 —— 课后习题数据（knowledge_exercise）
-- 本文件为《第2章Java编程基础.sql》的续篇，仅包含 knowledge_exercise 表数据，
-- 需在《第2章Java编程基础.sql》各表数据之后执行。
-- 依据用户提供的习题正文生成 120 行数据（id 50-169，承接第 1 章 id 1-49）：
--   基础习题2：选择题 39 题 + 填空题 35 题 + 简答题 20 题 + 分析题 9 题（共 103 题）
--   编程习题2：编程题 17 题
-- 字段：id, chapter_id, question, answer, difficulty, knowledge_point, page, page_order
-- chapter_id = 14（第 2 章一级章节）；knowledge_point 取自 knowledge_point 表 name 字段
-- difficulty：1-基础（选择题、填空题、简单编程题），2-进阶（简答题、分析题、综合编程题）
-- page 为整型教材页码（习题部分起始页为 P.76，源文档未标注各题分页，统一取 76）；
-- page_order 为基础习题2 与 编程习题2 合并后的连续编号（1-120）
-- 选择题答案为编者依据 Java 语法规则与教材内容判定；填空题/简答题答案为编者依据
-- 教材正文补充；分析题答案为编者给出的运行结果；编程题答案为编者编写的参考代码。
-- deleted/create_time/update_time 依赖表默认值，未显式赋值
-- ============================================================

-- 一、基础习题2 · 选择题（答案为编者依据 Java 语法与教材内容判定）
INSERT INTO `knowledge_exercise` (`id`, `chapter_id`, `question`, `answer`, `difficulty`, `knowledge_point`, `page`, `page_order`) VALUES
(50, 14, '1. 下列属于合法的 Java 标识符是( )。
(A) cat  (B) 5books  (C) +static  (D) −3.14159', 'A', 1, '标识符', 76, 1),
(51, 14, '2. 下列变量定义中，不合法的是( )。
(A) int $X;  (B) int _123;  (C) int summer_2010;  (D) int #dim;', 'D', 1, '标识符', 76, 2),
(52, 14, '3. 以下哪个不是 Java 的原始数据类型( )。
(A) int  (B) Boolean  (C) float  (D) char', 'B', 1, '布尔类型', 76, 3),
(53, 14, '4. 下列选项中，( )不属于 Java 语言的基本数据类型。
(A) 整数类型  (B) 数组类型  (C) 浮点类型  (D) 字符类型', 'B', 1, '整数类型', 76, 4),
(54, 14, '5. Java 语言使用的字符码集是( )。
(A) ASCII  (B) BCD  (C) DCB  (D) Unicode', 'D', 1, 'ASCII 码和 Unicode 码', 76, 5),
(55, 14, '6. 以下程序代码的输出结果是( )。
System.out.println(100/8);
(A) 12.5  (B) 12  (C) 4  (D) 0', 'B', 1, '算术运算符', 76, 6),
(56, 14, '7. 下列比较结果为 false 是( )。
(A) \'a\'>\'A\'  (B) \'a\'<\'b\'  (C) \'A\'<\'0\'  (D) \'X\'<\'Z\'', 'C', 1, '关系运算符', 76, 7),
(57, 14, '8. 假设创建了一个 Scanner 对象：
Scanner input = new Scanner(System.in);
下面哪个选项能读入一个双精度浮点数( )。
(A) input.Double();  (B) input.double();  (C) input.nextdouble();  (D) input.nextDouble();', 'D', 1, '输入基本数据类型', 76, 8),
(58, 14, '9. 将 1 赋给变量 x，下面正确的代码是( )。
(A) 1=x;  (B) x=1;  (C) x:=1;  (D) 1:=x;', 'B', 1, '赋值语句与赋值表达式', 76, 9),
(59, 14, '10. 以下哪个是不正确的字面值( )。
(A) 5_234e+1  (B) _2534  (C) 5_2.0  (D) 39d', 'B', 1, '浮点类型', 76, 10),
(60, 14, '11. 以下代码段执行后的输出结果为( )。
int x = -3; int y = -10;
System.out.println(y%x);
(A) −1  (B) -3  (C) 1  (D) 3', 'A', 1, '算术运算符', 76, 11),
(61, 14, '12. 以下代码段执行后的输出结果为( )。
int x=-3; int y=10;
System.out.println(y%x);
(A) −1  (B) -3  (C) 1  (D) 3', 'C', 1, '算术运算符', 76, 12),
(62, 14, '13. 以下程序的运行结果是( )。
public class Increment{
    public static void main(String args[]){
        int a=6;
        System.out.print(a);
        System.out.print(a++);
        System.out.print(a--);
    }
}
(A) 666  (B) 677  (C) 667  (D) 676', 'C', 1, '算术运算符', 76, 13),
(63, 14, '14. 设 x = 1，y = 2，z = 3，则表达式 y+=z--/++x 的值是( )。
(A) 3  (B) 3.5  (C) 4  (D) 5', 'A', 1, '增强赋值运算符', 76, 14),
(64, 14, '15. 已知 x = 2，y = 3，z = 4，则经过 z-=-y-x-- 运算后，z 的值为( )。
(A) 5  (B) 6  (C) 8  (D) 9', 'D', 1, '增强赋值运算符', 76, 15),
(65, 14, '16. 若定义 int a = 2，b = 2，下列表达式中值不为4的是( )。
(A) a*(++b)  (B) a*(b++)  (C) a+b  (D) a*b', 'A', 1, '算术运算符', 76, 16),
(66, 14, '17. 已知 a = 2，b = 3，则表达式 a%b*4%b 的值为( )。
(A) 2  (B) 1  (C) -1  (D) -2', 'A', 1, '算术运算符', 76, 17),
(67, 14, '18. 下列关于 boolean 类型的叙述中，正确的是( )。
(A) 可以将 boolean 类型的值转换为 int 类型的数值
(B) 不能将 boolean 类型的值转换为字符串
(C) 可以将 boolean 类型的值转换为 char 类型的值
(D) 不能将 boolean 类型的值转换为其他基本数据类型', 'D', 1, '布尔类型', 76, 18),
(68, 14, '19. 在 Java 中，"true"是( )。
(A) 一个 Java 关键字  (B) 一个布尔字面值  (C) 和 1 相同  (D) 和 0 相同', 'B', 1, '布尔类型', 76, 19),
(69, 14, '20. 给定 |x−2|≥4，下列哪个语句是等价的？( )
(A) x - 2 >= 4 && x - 2 <= -4
(B) x - 2 >= 4 || x - 2 <= -4
(C) x - 2 ≥ 4 && x - 2 ≤ -4
(D) x - 2 ≥ 4 || x - 2 ≤ -4', 'B', 1, '逻辑运算符', 76, 20),
(70, 14, '21. 1 + 1 + 1 + 1 + 1 == 5 的输出结果是( )。
(A) true  (B) false
(C) There is no guarantee that 1 + 1 + 1 + 1 + 1 == 5 is true.
(D) 不能保证 1 + 1 + 1 + 1 + 1 == 5 为真(true)。', 'A', 1, '关系运算符', 76, 21),
(71, 14, '22. 下面的陈述中，错误的是( )。
(A) (x>0 && x<10) 等同于 ((x>0) && (x<10))
(B) (x>0 || x<10) 等同于 ((x>0) || (x<10))
(C) (x>0 || x<10 && y<0) 等同于 (x>0 || (x<10 && y<0))
(D) (x>0 || x<10 && y<0) 等同于 ((x>0 || x<10) && y<0)', 'D', 1, '逻辑运算符', 76, 22),
(72, 14, '23. 假定 x = 4、y = 5，下列哪个结果是 true？( )
(A) x < 5 && y < 5  (B) x < 5 || y < 5
(C) x > 5 && y > 5  (D) x > 5 || y > 5', 'B', 1, '逻辑运算符', 76, 23),
(73, 14, '24. 下列与数学表达式 xy/3nz（分式：分子为 xy，分母为 3nz）对应的 Java 表达式不正确的是( )。
(A) x*y/3*n*z  (B) x*y/3/n/z  (C) x*y/(3*n*z)  (D) x/3*y/n/z', 'A', 1, '算术运算符', 76, 24),
(74, 14, '25. 执行下面代码后，y 的值是( )。
x = 0;
y = (x > 0) ? 10 : -10;
(A) 10  (B) −10  (C) 0  (D) 20', 'B', 1, '条件运算符', 76, 25),
(75, 14, '26. 三角函数方法 asin 的返回值范围是( )。
(A) 0～1  (B) −π/2～π/2  (C) 0～π  (D) −1/2～1/2', 'B', 1, '三角函数', 76, 26),
(76, 14, '27. 下列类型转换不合法的是( )。
(A) int i=\'a\';  (B) char ch=100;
(C) char ch=100l;  (D) char ch=(char)100;', 'C', 1, '数据类型转换', 76, 27),
(77, 14, '28. 下列哪个是不正确的字符直接值( )。
(A) \'1\'  (B) \'\\u345dE\'  (C) \'\\b\'  (D) \'\\uefff\'', 'B', 1, '字符类型', 76, 28),
(78, 14, '29. Math.sin(Math.PI) 返回值是( )。
(A) 0.0  (B) 1.0  (C) 0.5  (D) 0.4', 'A', 1, '三角函数', 76, 29),
(79, 14, '30. \'a\'的 unicode 码值是 97，\'c\'的 unicode 码值是( )。
(A) 96  (B) 97  (C) 98  (D) 99', 'D', 1, '字符类型', 76, 30),
(80, 14, '31. 下列( )语句会输出：smith\\exam1\\test.txt。
(A) System.out.println("smith\\exam1\\test.txt");
(B) System.out.println("smith\\\\exam1\\\\test.txt");
(C) System.out.println("smith\\"exam1\\"test.txt");
(D) System.out.println("smith\\"exam1\\"test.txt");', 'B', 1, '字符类型', 76, 31),
(81, 14, '32. 表达式 "Java" + 1 + 2 + 3 的值是( )。
(A) Java123  (B) Java 6  (C) java123  (D) 非法表达式', 'A', 1, '字符类型', 76, 32),
(82, 14, '33. 字符 A 的 Unicode 值是 65，表达式 "A" + 1 的值是( )。
(A) 66  (B) B  (C) A1  (D) 非法表达式', 'C', 1, '字符类型', 76, 33),
(83, 14, '34. 字符 A 的 Unicode 值是 65，表达式 \'A\' + 1 的值是( )。
(A) 66  (B) B  (C) A1  (D) 非法表达式', 'A', 1, '字符类型', 76, 34),
(84, 14, '35. 下列( )方法，将一个字符串 s 解析成一个整型值。
(A) integer.parseInt(s);  (B) Integer.parseInt(s);
(C) integer.parseInteger(s);  (D) Integer.parseInteger(s);', 'B', 1, '数据类型转换', 76, 35),
(85, 14, '36. 以下语句的输出结果是( )。
System.out.printf("%.1f",1234.56);
(A) 123.4  (B) 123.5  (C) 1234.5  (D) 1234.6', 'D', 1, '格式化控制台输出', 76, 36),
(86, 14, '37. Math.pow(2,3) 返回值为( )。
(A) 9  (B) 8  (C) 9.0  (D) 8.0', 'D', 1, '指数函数', 76, 37),
(87, 14, '38. 以下( )是不正确的数值型字面值。
(A) 5_234e+1  (B) _2534  (C) 5_2.0  (D) 39d', 'B', 1, '浮点类型', 76, 38),
(88, 14, '39. 将一个 double 型变量 d 赋值给一个 float 型变量 x，正确的语句是( )。
(A) x = (long)d  (B) x = (int)d;  (C) x = d;  (D) x = (float)d;', 'D', 1, '数据类型转换', 76, 39);

-- 二、基础习题2 · 填空题（答案为编者依据教材正文补充）
INSERT INTO `knowledge_exercise` (`id`, `chapter_id`, `question`, `answer`, `difficulty`, `knowledge_point`, `page`, `page_order`) VALUES
(89, 14, '1. 把字符映射到其二进制序列的过程称为__________。', '编码', 1, 'ASCII 码和 Unicode 码', 76, 40),
(90, 14, '2. __________规定了用从 0(0x00)到 127(0x7F)的 128 个数字来代表信息的规范编码，其中包括__________个控制码、一个空格码和 94 个形象码。', 'ASCII 码（美国信息交换标准代码）；33', 1, 'ASCII 码和 Unicode 码', 76, 41),
(91, 14, '3. __________占两个字节，能表示 65536 个字符，可用__________开头的 4 位 16 进制数表示。', 'Unicode 码；\\u', 1, 'ASCII 码和 Unicode 码', 76, 42),
(92, 14, '4. 美元符号$应该只在__________中使用，不要用来命名标识符。', '机器自动生成的源代码', 1, '标识符', 76, 43),
(93, 14, '5. Java 有两种类型的导入语句：__________和__________。', '特定导入（Specific Import）；通配符导入（Wildcard Import）', 1, '输入基本数据类型', 76, 44),
(94, 14, '6. 假设今天是星期三，200 天后是星期________。', '日（(3 + 200) % 7 = 0，0 表示星期日）', 1, '算术运算符', 76, 45),
(95, 14, '7. 编写一条语句将 45°转化为弧度值：____________________。', 'Math.toRadians(45)', 1, '三角函数', 76, 46),
(96, 14, '8. 编写一条语句将 π/5 转化为角度值：____________________。', 'Math.toDegrees(Math.PI / 5)', 1, '三角函数', 76, 47),
(97, 14, '9. Math.log(Math.exp(7.5)) 的结果是__________，Math.exp(Math.log(7.5)) 的结果是__________，Math.asin(Math.sin(Math.PI/6)) 的结果是__________，Math.sin(Math.asin(Math.PI/6)) 的结果是__________。', '7.5；7.5；π/6；π/6（后两项约 0.5236）', 1, '三角函数', 76, 48),
(98, 14, '10. 编写一个表达式，返回 25～100 之间的一个随机整数：____________________。', '25 + (int)(Math.random() * 76)', 1, '服务方法', 76, 49),
(99, 14, '11. 编写一个表达式，返回 12.5～125.5 之间的一个随机数：____________________。', '12.5 + Math.random() * 113', 1, '服务方法', 76, 50),
(100, 14, '12. 数学表达式：|x - 2| ≤ 4，用 Java 语言来表示，其形式是____________________。', 'Math.abs(x - 2) <= 4', 1, '逻辑运算符', 76, 51),
(101, 14, '13. 数学表达式：|y - 2²| ≥ 5，用 Java 语言来表示，其形式是____________________。', 'Math.abs(y - Math.pow(2, 2)) >= 5', 1, '逻辑运算符', 76, 52),
(102, 14, '14. 数学表达式：7/13×(x+y)×z^(1+3.5)（首项为分式 7/13），用 Java 语言表示，其形式是____________________。', '7.0 / 13 * (x + y) * Math.pow(z, 1 + 3.5)', 1, '算术运算符', 76, 53),
(103, 14, '15. 数学表达式：2y/(x+5)+(z+5)/(y-12)（两项分式之和），用 Java 语言表示，其形式是____________________。', '2.0 * y / (x + 5) + (z + 5) / (y - 12)', 1, '算术运算符', 76, 54),
(104, 14, '16. 如果 x = 2，y = x++ + ++x*2 + ++x*3，那么在赋值语句执行完后，x 的值是__________，y 的值是__________。', '5；25', 1, '算术运算符', 76, 55),
(105, 14, '17. 编写一个布尔表达式，当体重 weight 大于 120 小于 150 时结果为 true：____________________。', 'weight > 120 && weight < 150', 1, '逻辑运算符', 76, 56),
(106, 14, '18. 三角函数方法中的参数都是以弧度为单位的角，这句话是__________(正确/错误)的。', '正确', 1, '三角函数', 76, 57),
(107, 14, '19. 编写语句显示 3^2.5 的计算结果：____________________。', 'System.out.println(Math.pow(3, 2.5));', 1, '指数函数', 76, 58),
(108, 14, '20. 27/4 的结果是__________，27/4.0 的结果是__________。', '6；6.75', 1, '算术运算符', 76, 59),
(109, 14, '21. Jshell 是自 Java SE 9 开始引入的一个新特性—命令行交互工具，以__________方式交互式评估 Java 语言的声明、语句和表达式。', '交互', 1, '命令行交互工具 JShell', 76, 60),
(110, 14, '22. Jshell 可以使用命令__________打开一个编辑器，使用命令__________退出。', '/edit；/exit', 1, '命令行交互工具 JShell', 76, 61),
(111, 14, '23. Java 数据类型分为__________和__________。', '基本数据类型（原始数据类型）；引用数据类型', 1, '整数类型', 76, 62),
(112, 14, '24. 浮点型直接量带小数点，默认情况下是__________。', 'double 型', 1, '浮点类型', 76, 63),
(113, 14, '25. 给出以下代码的输出：
double amount=5;
System.out.print(amount/2);
System.out.println(","+5/2);
结果为__________。', '2.5,2', 1, '算术运算符', 76, 64),
(114, 14, '26. 请使用类型转换将 double 类型变量 d 的数值四舍五入为一个整数值，并赋给整型变量 i：____________________。', 'i = (int)(d + 0.5);', 1, '数据类型转换', 76, 65),
(115, 14, '27. 请使用数学函数将 double 类型变量 d 的数值四舍五入为一个整数值，并赋给整型变量 i：____________________。', 'i = (int)Math.round(d);', 1, '服务方法', 76, 66),
(116, 14, '28. 如果忘记为字符串加引号，将产生__________错误。', '编译（语法）', 1, '编译错误', 76, 67),
(117, 14, '29. 假设编写一个计算矩形周长的程序，但是错误地计算了矩形面积的程序，这属于__________错误。', '逻辑', 1, '逻辑错误', 76, 68),
(118, 14, '30. 编程错误可以分为三类：__________、__________和__________。', '编译错误；运行时错误；逻辑错误（注：教材将程序设计错误分为编译错误、运行时错误和逻辑错误三类）', 1, '编译错误', 76, 69),
(119, 14, '31. 编译器报告的错误称为__________错误。__________错误是指引起程序非正常结束的错误。当一个程序没有按照预期的方式执行时，产生__________错误。', '编译；运行时；逻辑', 1, '编译错误', 76, 70),
(120, 14, '32. 一个算法有 5 个重要特性：__________、__________、__________、有输入、有输出。', '有穷性；确定性；可行性', 1, '算法', 76, 71),
(121, 14, '33. ____________________是对程序代码排版所表现出来的外观样式。', '程序设计风格（programming style）', 1, '程序设计风格', 76, 72),
(122, 14, '34. 代码块的写法有两种常用风格：__________和__________。', '行尾风格；次行风格', 1, '程序设计风格', 76, 73),
(123, 14, '35. Java 文档注释是用来生成 API(应用程序接口)文档的。Java 文档注释以__________开始，并以__________结束，通过使用__________命令提取到一个 HTML 文件。', '/**；*/；javadoc', 1, '注释', 76, 74);

-- 三、基础习题2 · 简答题（答案为编者依据教材正文补充）
INSERT INTO `knowledge_exercise` (`id`, `chapter_id`, `question`, `answer`, `difficulty`, `knowledge_point`, `page`, `page_order`) VALUES
(124, 14, '1. 什么是 ASCII 码？', 'ASCII 码（American Standard Code for Information Interchange，美国信息交换标准代码）是一种使用 7 位二进制数字的编码方案，规定了用从 0(0x00)到 127(0x7F)的 128 个数字来代表信息的规范编码，其中包括 33 个控制码、1 个空格码和 94 个形象码。', 2, 'ASCII 码和 Unicode 码', 76, 75),
(125, 14, '2. 什么是 Unicode 码？', 'Unicode 码（统一码）是一种 16 位的编码方案，占两个字节，可以表示 65536 个字符，从 \'\\u0000\' 到 \'\\uFFFF\'，能够表示世界上大多数语言的文字符号，Java 语言采用 Unicode 码作为字符码集。', 2, 'ASCII 码和 Unicode 码', 76, 76),
(126, 14, '3. Java 语言的字母包括什么？', 'Java 语言的字母包括大写字母 A～Z、小写字母 a～z、下划线（_）、美元符号（$）以及 Unicode 字符集中表示字母的字符。标识符只能由 Java 字母和数字 0～9 组成，且不能以数字开头。', 2, '标识符', 76, 77),
(127, 14, '4. 什么是关键字？列举 10 个关键字。', '关键字（keyword）是 Java 语言中已经被赋予特定含义、由系统保留使用的单词（也称保留字），不能用作标识符。例如：class、public、static、void、int、char、boolean、double、float、final（还可以列举 byte、short、long、if、else、while、for、return、package、import、goto 等）。', 2, '关键字', 76, 78),
(128, 14, '5. 以下标识符哪些是合法的？哪些是 Java 的关键字？
你好, 3star, xx, exports, Hello, SHello, _Hello, #hello, super, goto, int, x, y, float', '合法的标识符：你好、xx、exports、Hello、SHello、_Hello、x、y；其中是 Java 关键字的：super、goto、int、float；不合法的：3star（以数字开头）、#hello（含非法字符 #）。', 2, '标识符', 76, 79),
(129, 14, '6. 使用常量有什么好处？声明一个 int 类型的常量 Height，值为 180。', '使用常量的好处：（1）常量一经初始化后其值不能再被修改，可以防止程序执行过程中误改；（2）当该值需要修改时，只需修改常量声明处一处即可；（3）用有意义的名字代替直接写在代码中的数值（魔法数字），提高程序的可读性和可维护性。声明：final int Height = 180;', 2, '常量', 76, 80),
(130, 14, '7. 赋值表达式中左侧变量的数据类型与右侧表达式值的数据类型应兼容，兼容的含义是什么？', '兼容的含义是：左侧变量的数据类型与右侧表达式值的数据类型要么一致，要么右侧表达式值的数据类型能够隐式（自动）转换为左侧变量的数据类型。', 2, '数据类型转换', 76, 81),
(131, 14, '8. 以下哪些是合法的浮点类型值？
4_23_1E+5, _34.345d, 345_452_433.455, 5_.345, 45F, 45d, 6.345_453', '合法的浮点类型值：4_23_1E+5、345_452_433.455、45F、45d、6.345_453；不合法的：_34.345d（下划线不能位于开头）、5_.345（下划线必须位于两个数字之间，不能与小数点相邻）。', 2, '浮点类型', 76, 82),
(132, 14, '9. float 类型和 double 类型的变量存储的数值，其有效数字位数分别是多少？', 'float 类型的变量存储的数值有 7 位有效数字（约精确到小数点后 6～7 位），double 类型的变量存储的数值有 15 位有效数字。double 型的精度高于 float 型。', 2, '浮点类型', 76, 83),
(133, 14, '10. 假设今天是星期四，150 天后是星期几？请列出求余计算公式并计算结果。', '求余公式：星期数 = (今天的星期数 + 天数) % 7，其中 0～6 依次表示星期日～星期六。计算：(4 + 150) % 7 = 154 % 7 = 0，所以 150 天后是星期日。', 2, '算术运算符', 76, 84),
(134, 14, '11. 假设某个月 1 日是星期二，这个 23 日是星期几？请列出求余计算公式并计算结果。', '求余公式：星期数 = (1 日的星期数 + (n - 1)) % 7，其中 0～6 依次表示星期日～星期六。计算：(2 + (23 - 1)) % 7 = 24 % 7 = 3，所以 23 日是星期三。', 2, '算术运算符', 76, 85),
(135, 14, '12. 下面导入语句有无性能差异？
import java.util.*;
import java.util.Scanner;', '没有性能差异。特定导入和通配符导入在性能上没有什么差别：导入语句只是告诉 Java 编译器在哪里能够找到程序中需要用到的类，并不是直接将包中所有类的信息加载到 JVM 中，只有在程序中用到相应的类时，这些类才会被 JVM 加载。通配符导入的唯一不便是如果多个包中以星号导入，可能引起类名冲突。', 2, '输入基本数据类型', 76, 86),
(136, 14, '13. 分析下面代码中的错误。
public class TestAnalyze2_4{
    public static void main(String[] args){
        int x = y + 1;
        System.out.println(x);
    }
}', '错误：语句 int x = y + 1; 中变量 y 在使用之前既未声明也未初始化，编译器报告"找不到符号（y）"的编译错误。应先声明并初始化变量 y，例如：int y = 2; 然后再执行 int x = y + 1;。', 2, '编译错误', 76, 87),
(137, 14, '14. 请指出并修改下面代码中的错误。
pulibc class Main {
    public static void main(String[] args){
        int i = j = k = 3;
        System.out.println(i + "" + j + "" + k);
    }
}', '错误有两处：（1）pulibc 是关键字 public 的拼写错误；（2）int i = j = k = 3; 中变量 j、k 在使用之前未声明，产生编译错误。修改后的代码：
public class Main {
    public static void main(String[] args){
        int i, j, k;
        i = j = k = 3;
        System.out.println(i + " " + j + " " + k);
    }
}', 2, '编译错误', 76, 88),
(138, 14, '15. Math 类为什么不需要导入？', '因为 Math 类位于 java.lang 包中，而 java.lang 包是 Java 程序默认（隐式）导入的包，该包中所有的类编译器都会自动导入，因此程序中可以直接使用 Math 类，而不需要使用 import 语句显式导入。', 2, '三角函数', 76, 89),
(139, 14, '16. 将一个 double 类型数值显式转换为 int 类型时，double 类型数值的小数部分是如何处理的？类型转换有无改变被类型转换的变量？', '将 double 类型数值显式转换为 int 类型时，小数部分被直接截断（舍弃），而不是四舍五入，例如 (int)3.9 的值是 3，(int)-3.9 的值是 -3。类型转换不会改变被转换变量本身的值，例如 double d = 3.9; int i = (int)d; 之后 d 的值仍然是 3.9，类型转换只是用变量的值计算产生一个新的指定类型的值。', 2, '数据类型转换', 76, 90),
(140, 14, '17. 下面类型转换是否合法？请解释。
① boolean b1 = 0;
② boolean b2 = false; int a = (int)b2;
③ int i = 65; char c = (char) i;
④ float f = 345.67f; int i = (float)f;
⑤ char c = \'a\'; int i = (int)c;
⑥ int a = 5.5;', '① 不合法：boolean 类型只能取 true 或 false，不能接受 int 类型的值 0。
② 不合法：boolean 类型不能与任何数值类型相互转换，(int)b2 是错误的。
③ 合法：int 到 char 的转换需要显式（强制）类型转换，c 的值为 \'A\'。
④ 不合法：float 到 int 是取值范围变小的转换，必须显式转换为 (int)f，写成 (float)f 后赋给 int 型变量仍然类型不兼容。
⑤ 合法：char 到 int 的转换可以隐式进行，也可以显式转换，i 的值为 97。
⑥ 不合法：double 型字面值 5.5 不能隐式转换为 int，必须写成 int a = (int)5.5;（截断后 a 的值为 5）。', 2, '数据类型转换', 76, 91),
(141, 14, '18. 在编码时，遗漏了左括号会产生什么类型的错误？', '会产生编译错误（属于语法错误）。编译器在编译阶段检查语法的正确性，遗漏左括号时编译器会报告括号不匹配（如"需要 ("）之类的错误信息，程序无法通过编译。', 2, '编译错误', 76, 92),
(142, 14, '19. 程序要求的功能是计算圆球的体积，但是最后计算的结果是圆球的表面积，且程序可以正常运行。这是什么类型的错误？', '这是逻辑错误。程序能够正常通过编译并运行，不会报告任何错误，但程序的执行结果与预期的功能要求（计算体积）不符，这类错误称为逻辑错误。', 2, '逻辑错误', 76, 93),
(143, 14, '20. 一个程序要求输入一个浮点数，用户不小心输入了一个带英文字母的字符串，运行该程序会产生什么类型的错误？', '会产生运行时错误。程序可以正常编译，但在运行过程中，Scanner 读取浮点数时无法将带英文字母的字符串解析为数值，抛出 InputMismatchException（输入不匹配异常），导致程序非正常结束。', 2, '运行时错误', 76, 94);

-- ============================================================
-- 文件名：第2章Java编程基础_difficult.sql
-- 表    ：knowledge_difficult_point（教材重难点表）
-- 说    明：本章重难点为人工总结的测试数据（源文档无明确"难点"标注），
--           仅用于多表联查测试，不是源文档逐字提取内容。
-- 外键说明：chapter_id = 14，对应 knowledge_chapter 表中"第2章 Java 编程基础"；
--           page 取值均落在 knowledge_point 表对应知识点的 page_ref 页码范围内
--           （第 2 章正文页码 P.26~P.76）。
-- id    ：1 ~ 20（第 1 章该表无数据，全表自 1 起编号）。
-- type  ：1-易错（9 条）；2-高频考点（7 条）；3-概念辨析（4 条）。
-- 生成日期：2026-09-17
-- ============================================================

INSERT INTO `knowledge_difficult_point` (`id`, `chapter_id`, `description`, `type`, `page`, `page_order`) VALUES
(1, 14, '标识符命名规则：只能以 Java 字母（Unicode 字母、$、下画线）开头，Java 数字不能出现在首位；不能用保留关键字命名，$ 一般仅由编译器生成使用，编程中应避免。', 2, 28, 1),
(2, 14, '保留关键字与上下文关键字辨析：51 个保留关键字（由 ASCII 字符组成）不能用作标识符；16 个上下文关键字（如 var）是否被解释为关键字取决于出现的上下文。', 3, 29, 1),
(3, 14, '局部变量必须先声明并初始化后使用：未初始化的局部变量直接读取会导致编译错误；变量作用域从声明处开始，到所在代码块结束。', 1, 29, 2),
(4, 14, '赋值号 = 与相等比较 == 的混淆：赋值本身是表达式且有值，链式赋值 a = b = c 从右向左求值；把 == 误写成 = 是初学者最常见错误之一。', 1, 31, 1),
(5, 14, '整型字面值与进制：0 开头为八进制、0x/0X 开头为十六进制、0b/0B 开头为二进制；long 类型字面值需加 L 后缀，否则默认按 int 处理。', 2, 37, 1),
(6, 14, '整型溢出：int 运算结果超出取值范围（-2147483648~2147483647）时发生回绕而不报错，大数运算应改用 long 或注意取值范围。', 1, 38, 1),
(7, 14, '浮点型字面值默认为 double：float 字面值必须加 F/f 后缀；float 有效位数约 6~7 位，double 约 15 位，精度不足会导致比较失真。', 1, 39, 1),
(8, 14, '浮点数精度问题：IEEE 754 二进制无法精确表示 0.1 等十进制小数，两个浮点数是否相等不能用 == 直接判断，应判断差的绝对值是否小于给定误差范围。', 2, 40, 1),
(9, 14, 'char 类型的双重身份：本质是无符号 16 位整数（Unicode 码点），可与整数混合运算且结果为 int；大写字母与对应小写字母码点相差 32，字符相加得到整数而非字符串，属常见概念混淆点。', 3, 40, 2),
(10, 14, '转义序列辨析：\\n 为换行、\\t 为制表符、\\\\ 为反斜杠本身；转义序列书写错误（如将 \\n 误写为 /n）会改变字符含义或直接导致编译错误。', 1, 41, 1),
(11, 14, '隐式类型转换方向：byte、short、char 参与运算时自动提升为 int，随后按 long→float→double 提升；把大范围类型赋给小范围类型必须强制转换且可能丢失精度。', 2, 42, 1),
(12, 14, '整数除法与浮点除法：两个整数相除结果直接舍弃小数部分（1/2 的值为 0），任一操作数为浮点数则按浮点除法（1.0/2 的值为 0.5）；混合表达式中求值顺序不同则结果不同。', 1, 52, 1),
(13, 14, '求余运算符 % 的符号规则：余数符号与左操作数（被除数）一致，如 55%6=1、-55%6=-1、-55%-6=-1；浮点数也可取余，如 55.5%6=3.5。', 1, 53, 1),
(14, 14, '前缀 ++/-- 与后缀 ++/-- 的区别：前缀先自增（减）再取值，后缀先取值再自增（减）；在复合表达式（如 j = ++i + i++ * 2）中求值顺序直接决定最终结果，是本章最大易错点。', 1, 54, 1),
(15, 14, '增强赋值运算符隐含强制类型转换：byte b = 1; b += 1; 合法（等价于 b = (byte)(b + 1)），而 b = b + 1; 不合法（右侧已提升为 int），两种写法并不等价。', 1, 56, 1),
(16, 14, '短路逻辑运算符 &&、|| 与非短路 &、| 的区别：短路运算在左侧已能确定结果时不再求值右侧，可避免异常或副作用；&、| 永远对两个操作数都求值，混用会改变程序行为。', 2, 58, 1),
(17, 14, 'Math 类三角函数使用弧度而非角度：角度需先经 Math.toRadians() 转换；反三角函数（如 acos）返回值范围为 [0, π]，注意与角度的换算。', 3, 60, 1),
(18, 14, 'Math.rint 与 Math.round 的舍入差异：rint 按四舍六入五取偶（银行家舍入）规则舍入，如 rint(3.5)=4 而 rint(2.5)=2；round 则是加 0.5 后向下取整。', 3, 63, 1),
(19, 14, 'System.out.printf 格式标识符：%d 整数、%f 浮点数、%.2f 保留两位小数、%s 字符串、%b 布尔值；格式标识符与参数列表必须一一对应，类型不匹配会抛出异常。', 2, 49, 1),
(20, 14, '三类程序设计错误的区分：编译错误（语法问题）在编译期发现；运行时错误（如整数除零）在执行期抛出异常终止程序；逻辑错误编译运行均不报错但结果不正确，最难发现和调试。', 2, 74, 1);

