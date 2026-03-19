/*
 Navicat Premium Data Transfer

 Source Server         : localhost的3306
 Source Server Type    : MySQL
 Source Server Version : 80025
 Source Host           : localhost:3306
 Source Schema         : history_creation

 Target Server Type    : MySQL
 Target Server Version : 80025
 File Encoding         : 65001

 Date: 19/03/2026 20:38:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for creation_version
-- ----------------------------
DROP TABLE IF EXISTS `creation_version`;
CREATE TABLE `creation_version`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `work_id` bigint(0) NOT NULL,
  `user_id` bigint(0) NOT NULL,
  `content_snapshot` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `version_number` int(0) NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_work`(`work_id`) USING BTREE,
  CONSTRAINT `fk_version_work` FOREIGN KEY (`work_id`) REFERENCES `creation_work` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '创作版本' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of creation_version
-- ----------------------------

-- ----------------------------
-- Table structure for creation_work
-- ----------------------------
DROP TABLE IF EXISTS `creation_work`;
CREATE TABLE `creation_work`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '未命名',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分组名称',
  `last_opened_at` datetime(0) NULL DEFAULT NULL COMMENT '最近打开时间',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `pinned_outline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '锁定故事大纲',
  `char_profiles` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '人物设定档案',
  `outline_data` json NULL COMMENT '结构化大纲数据（JSON）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  CONSTRAINT `fk_work_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '创作作品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of creation_work
-- ----------------------------
INSERT INTO `creation_work` VALUES (28, 4, '第一章 变故', '暮春的雨丝斜织着，将巷口那方“松风”茶幌浸得绵软。院中两株金桂抽了新叶，叶片上的水珠坠在青石板缝里，洇出浅浅的痕。正堂的榉木案几上，半开的锡制茶罐敞着口，陈年的雨前龙井混着西窗下熬药的苦香漫开来——那只粗陶药罐正咕嘟着，蒸汽裹着当归的气味攀附到梁间，与悬着的蚕茧串碰出细碎的响。东厢房的雕花槛窗半阖，漏进的风掀起案头未合的账册，纸页上的朱笔批注还沾着晨间的露，像极了老主事咳在帕子上的血渍。\n\n檐角的铜铃被风撞得轻响，惊起院中栖在桂枝上的雀儿，扑棱着翅膀钻进雨幕里。东厢房里，老主事枯瘦的手搭在案上，指节泛着青灰，方才掀账册时带起的风，又惹得他低咳两声，帕子上的红痕比晨间更深了些。西窗下，熬药的丫鬟正踮脚添了把柴，灶膛里的火舌舔着药罐底，当归的苦香愈发浓重，混着堂前飘来的龙井清冽，竟透出几分说不出的涩意。巷口传来木屐踩过积水的啪嗒声，有人提着竹篮经过，篮沿滴下的水珠砸在青石板上，与药罐的咕嘟声、铜铃的脆响叠在一起，倒让这暮春的午后，多了几分烟火气的暖，只是那暖里，总裹着挥不去的沉郁，像极了梁间悬着的蚕茧串，看似静默，实则藏着化不开的愁绪。\n\n\n\n', '茶色生香', '2026-03-19 18:30:45', '2026-03-19 18:27:54', '2026-03-19 18:30:17', NULL, NULL, NULL);
INSERT INTO `creation_work` VALUES (29, 4, '采茶女', '\n　　晨雾未散时，西坡的茶垄已浮起一层淡绿的烟。穿青布短衫的妇人沿着垄沟挪步，蓝布帕子扎在发尾，腕间银镯碰着臂弯里的竹篮，叮出细碎的响。她指尖停在茶枝上——那芽尖才抽一寸，卷着鹅黄的边，像刚醒的雀儿舌头，沾着晨露，凉得指腹微微一缩。“咔”的一声，嫩芽落进竹篮，撞在已有的新芽上，发出极轻的脆响，混着坡下溪涧的流水声，飘得比雾还远。  \n\n　　鼻端先漫开茶的清苦，接着是篱边野蔷薇的甜香，还有泥土被夜露浸透的腥润，缠着她的衣角往衣领里钻。日头从东边松梢爬上来，雾丝一缕缕褪成白汽，茶垄的绿忽然亮了——每片新芽都泛着柔润的光，像撒了一把刚磨好的碎玉。她的袖口沾了茶汁，染成淡淡的碧，却不在意，只弯腰继续掐——竹篮里的芽堆渐次厚起来，每一片都带着晨露的痕，等着晒过三伏天的日头，变成瓷碗里浮起的香。  \n\n　　风掠过茶垄，带起几片没掐完的嫩叶，飘到她的帕子上。她抬头望了眼日头，嘴角抿出点笑——这筐新芽，够换半升糙米，够给灶上的女儿煮碗糖茶。远处传来邻村阿姐的山歌，调子软得像茶芽上的露，她跟着哼了两句，指尖又快了些。\n\n　　忽闻坡下脚步沓杂，原是邻家阿嫂挎着柳条筐上来。\"翠姐儿早啊，今儿这芽尖掐得比露水还透亮。\"阿嫂抹了把额角汗珠，腕间铜铃叮当作响。她笑着应了，将篮中新摘的茶青拨些过去：\"昨儿后晌见你灶上烟稀，拿回去掺着陈茶熬汤罢。\"日头渐高，茶垄间的阴影缩成细线，竹篮已沉甸甸压弯臂肘。她解下蓝布帕拭汗，忽觉指节泛酸——去年此时还能单手攀上崖边老枞，如今倒要扶着茶树歇气。\n\n　　山下炊烟袅袅升起，混着柴火味飘上坡来。她数着篮中茶芽，够换半升糙米，余下的晒干收进陶罐，待腊月里女儿归宁，正好沏壶好茶。远处传来更夫敲梆声，三下两下，惊起茶林里的斑鸠。她弯腰提起竹篮，裙裾扫过沾露的野草，留下一串深浅不一的脚印，像岁月在茶山上刻下的年轮。\n\n\n', NULL, '2026-03-19 18:46:35', '2026-03-19 18:31:22', '2026-03-19 18:50:14', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for favorite_group
-- ----------------------------
DROP TABLE IF EXISTS `favorite_group`;
CREATE TABLE `favorite_group`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分组名称',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_name`(`user_id`, `name`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_fav_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏分组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorite_group
-- ----------------------------

-- ----------------------------
-- Table structure for feedback
-- ----------------------------
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/read/resolved',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_created`(`created_at`) USING BTREE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of feedback
-- ----------------------------
INSERT INTO `feedback` VALUES (1, 6, '6', 'sdfsd', 'resolved', '2026-03-15 01:14:04', '2026-03-19 16:14:06');

-- ----------------------------
-- Table structure for material
-- ----------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史沉淀/传统民俗/服饰装扮/行业手艺/宗教信仰/兵器武林/饮食文化/玉石珍宝/传说典故/科技文明/五行异象/其他',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `source_user_id` bigint(0) NULL DEFAULT NULL COMMENT '若为用户投稿素材，则记录提交人',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED=正式; PENDING=待审核; REJECTED=已驳回',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史特色素材' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material
-- ----------------------------
INSERT INTO `material` VALUES (3, '饮食文化', '茶', '中国现代名茶有数百种之多，根据其历史分析，有下列三种情况：\n\n　　有一部分属传统名茶，如西湖龙井、庐山云雾、洞庭碧螺春、黄山毛峰、太平猴魁、恩施玉露、信阳毛尖、六安瓜片、屯溪珍眉、老竹大方、桂平西山茶、君山银针、云南普洱茶、苍梧六堡茶、政和白毫银针、白牡丹、安溪铁观音、凤凰水仙、闽北水仙、武夷岩茶、祁门红茶等。\n\n　　另一部分是恢复历史名茶，也就是说历史上曾有过这类名茶，后来未能持续生产或已失传的，经过研究创新，恢复原有的茶名。如休宁松罗、涌溪火青、敬亭绿雪、九华毛峰、龟山岩绿、蒙顶甘露、仙人掌茶、天池茗毫、贵定云雾、青城雪芽、蒙顶黄芽、阳羡雪芽、鹿苑毛尖、霍山黄芽、顾渚紫笋、径山茶、雁荡毛峰、日铸雪芽、金奖惠明、金华举岩、东阳东白等等。\n\n　　还有大部分是属于新创名茶，如婺源茗眉、南京雨花茶、无锡毫茶、茅山青峰、天柱剑毫、岳西翠兰、齐山翠眉、望府银毫、临海蟠毫、千岛玉叶、遂昌银猴、都匀毛尖、高桥银峰、金水翠峰、永川秀芽、上饶白眉、湄江翠片、安化松针、遵义毛峰、文君绿茶、娥眉毛峰、雪芽、雪青、仙台大白、早白尖红茶、黄金桂、秦巴雾毫、汉水银梭、八仙云雾、南糯白毫、午子仙毫等等。\n\n　　乌龙茶是中国茶的代表,是一种半发酵的茶,透明的琥珀色茶汤是其特色。\n\n　　最能体现中国茶文化特色的当数乌龙茶，因为乌龙茶的品尝比较讲究，冲泡也颇费工夫，因而人们称它为“功夫茶”。\n\n　　我国是茶树的原产地，茶树最早出现于我国西南部的云贵高原、西双版纳地区。但是有部分学者认为茶树的原产地在印度，理由是印度有野生茶树，而中国没有。但他们不知中国在公元前200年左右的《尔雅》中就提到有野生大茶树，而且还有“茶树王”。\n\n　　《神农本草经》是我国的第一部药学专著，自战国时代写起，成书于西汉年间。这部书以传说的形式，搜集自远古以来，劳动人民长期积累的药物知识，其中有这样的记载：“神农尝百草，日遇七十二毒，得荼而解之”。据考证：这里的荼是指古代的茶，大意是说，远在上古时代，传说中的炎帝，亲口尝过百草，以便从中发现有利于人类生存的植物，竟然一天之内多次中毒。但由于服用茶叶而得救。这虽然是传说，带有明显的夸张成份，但也可从中得知，人类利用茶叶，可能是从药用开始的。\n\n　　据考察，“茶”字最早出现在《百声大师碑》和《怀晖碑》中，时间大约在唐朝中期，公元806年到公元820年前后，在此之前，“茶”是用多义字“荼”表示的。\n\n　　“茶”字的基本意义是“苦菜”，上古时期人们对茶还缺乏认识，仅仅根据它的味道，把它归于苦菜一类，是完全可以理解的，当人们认识到它与一般苦菜的区别及其特殊功能时，单独表示它的新字也就产生了。\n\n　　茶与粮食，占有同等重要的位置。可是，“由于气候等原因，当地并不产茶，官府为了增强控制少数民族的力量，对茶叶的供给采取限量，直接分配的办法，以求达到“以茶治边”的目的。与此同时，官府不仅控制茶叶的供应，而且，以少量的茶，交换多数的战马，给兄弟民族带来沉重的负担，这就是历史上的“茶马互市”。\n\n　　茶叶作为一种饮料，从唐朝开始，流传到我国西北各个少数民族地区，成为当地人民生活的必需品，“一日无茶则滞，三日无茶则病”。中国是茶树的原产地。然而，中国在茶业上对人类的贡献，主要在于最早发现了茶这种植物，最先利用了茶这种植物，并把它发展形成为我国和东方乃至整个世界的一种灿烂独特的茶文化。如我国史籍所载，在未知饮茶前，“古人夏则饮水，冬则饮汤”，恒以温汤生水解渴。以茶为饮则改变了人们喝生水的陋习，较大地提高了人民的健康水平。至于茶在欧美一带，被认为“无疑是东方赐予西方的最好礼物”，“欧洲若无茶与咖啡之传入，饮酒必定更加无度”，“茶给人类的好处无法估计”，“我确信茶是人类的救主之一”，“是伟大的慰藉品”等等。世界各国饮茶及茶的生产和贸易，除朝鲜、日本以及中亚、西亚一带是唐朝前后就从中国传入者外，其他多是16世纪以后，特别是近200年以来才传入发展起来的。\n\n　　中国茶文化\n\n　　中国是茶的故乡，制茶、饮茶已有几千年历史，名品荟萃，主要品种有绿茶、红茶、乌龙茶、花茶、白茶、黄茶、黑茶。茶有健身、治疾之药物疗效，又富欣赏情趣，可陶冶情操。品茶、待客是中国个人高雅的娱乐和社交活动，坐茶馆、茶话会则是中国人社会性群体茶艺活动。中国茶艺在世界享有盛誉，在唐代就传入日本，形成日本茶道。\n\n　　饮茶始于中国。茶叶冲以煮沸的清水，顺乎自然，清饮雅尝，寻求茶的固有之味，重在意境，这是茶的中式品茶的特点。同样质量的茶叶，如用水不同、茶具不同或冲泡技术不一，泡出的茶汤会有不同的效果。我国自古以来就十分讲究茶的冲泡，积累了丰富的经验。泡好茶，要了解各类茶叶的特点，掌握科学的冲泡技术，使茶叶的固有品质能充分地表现出来。\n\n　　中国人饮茶，注重一个“品”字。“品茶”不但是鉴别茶的优劣，也带有神思遐想和领略饮茶情趣之意。在百忙之中泡上一壶浓茶，择雅静之处，自斟自饮，可以消除疲劳、涤烦益思、振奋精神，也可以细啜慢饮，达到美的享受，使精神世界升华到高尚的艺术境界。品茶的环境一般由建筑物、园林、摆设、茶具等因素组成。饮茶要求安静、清新、舒适、干净。中国园林世界闻名，山水风景更是不可胜数。利用园林或自然山水间，搭设茶室，让人们小憩，意趣盎然。\n\n　　中国是文明古国，礼仪之邦，很重礼节。凡来了客人，沏茶、敬茶的礼仪是必不可少的。当有客来访，可争求意见，选用最合来客口味和最佳茶具待客。以茶敬客时，对茶叶适当拼配也是必要的。主人在陪伴客人饮茶时，要注意客人杯、壶中的茶水残留量，一般用茶杯泡茶，如已喝去一半，就要添加开水，随喝随添，使茶水浓度基本保持前后一致，水温适宜。在饮茶时也可适当佐以茶食、糖果、菜肴等，达到调节口味和点心之功效。\n\n　　世界各国的制茶技术，均直接或间接地来自我国。\n\n　　805年：唐代时，日本和尚最澄大师及806年空海大师，留学我国研究佛学，归国后，将我国茶叶蒸青绿茶的制茶技术传入日本。\n\n　　1811年：荣西和尚留学回归日本，将锅炒茶制法传入日本。\n\n　　1828至1833年：茶叶产制技术传入印尼。荷属东印度公司派茶师杰哥逊前后六次来我国学习研究，每次均带回茶种、制茶技术工人及器具。\n\n　　1833年：苏俄来茶我国采购茶籽与茶苗，1848年开始采摘，依照我国茶叶制作方法开始生产。\n\n　　1834年：印度成立植茶研究发展委员会，即派秘书哥登来我国学习茶叶产制技术。购买茶籽及茶苗，并寻找、招收四川省雅州及福建省武夷山等地茶师及工人，到大吉岭等地发展茶业。\n\n　　1835年：宇治山本氏，传回我国覆盖茶园“玉露茶”的制法。\n\n　　1836年：哥登氏带回我国茶工，在阿萨姆勃鲁茶厂中，按照我国红茶制法，试制成功，日后发展成今天的阿萨姆红茶。\n\n　　1866年：斯里兰卡正式制茶始于特罗氏。学习我国武夷岩茶制法，试制成功。至1873年后才仿效印度的机械制法。\n\n　　1877至1887年：南非及东非洲茶叶的发展，已由我国输入茶叶生产技术。\n\n　　1898年：日本开始仿制我国红茶、绿砖茶。\n\n　　1926年：日本仿效我国珠茶制法。日本最普遍的煎茶，是仿自我国浙江龙井。\n\n　　1949年：第二次世界大战后，英国茶业者等退出印度、锡兰的茶叶经营，将技术与资本等，转移投资于肯尼亚等新茶区的开阔，才大量生产红茶。\n\n　　我国是茶的故乡，有着悠久的种茶历史，又有着严格的敬茶礼节，还有着奇特的饮茶风俗。我国饮茶，从神农时代开始，少说也有四千七百多年了。茶礼有缘，古已有之。\n\n　　客来敬茶，这是我国汉族同胞，最早重情好客的传统美德与礼节。直到现在，宾客至家，总要沏上一杯香茗。喜庆活动，也喜用茶点招待。开个茶话会，既简便经济，又典雅庄重。所谓君子之交淡如水，也是指清香宜人的茶水。\n\n　　我国汉族同胞还有种种以茶代礼的风俗。南宋都城杭州，每逢立夏，家家各烹新茶，并配以各色细果，馈送亲友毗邻，叫做七家茶。这种风俗，就是在茶杯内放两颗青果即橄榄或金桔，表示新春吉祥如意的意思。\n\n　　茶礼还是我国古代婚礼中一种隆重的礼节的。明?许次纾在《茶疏考本》中说：茶不移本，植必子生。古人结婚以茶为识，以为茶树只能从种子萌芽成株，不能移植，否则就会枯死，因此把茶看作是一种至性不移的象征。所以，民间男女订婚以茶为礼，女方接受男方聘礼，叫下茶或茶定，有的叫受茶，并有一家不吃两家茶的谚语。同时，还把整个婚姻的礼仪总称为三茶六礼。三茶，就是订婚时的下茶，结婚的定茶，同房时的合茶。下茶又有男茶女酒之称，即定婚时，男家除送如意压帖外，要回送几缸绍兴酒。婚礼时，还要行三道茶仪式。三道茶者，第一杯百果，第二杯莲子、枣儿；第三杯方是茶。吃的方式，接杯之后，双手捧之，深深作揖，然后向嘴唇一触，即由家人收去。第二道亦如此。第三道，作揖后才可饮。这是最尊敬的礼仪。这些繁俗，现在当然没有了，但婚礼的敬茶之礼，仍沿用成习。\n\n　　张源《茶录》“汤辨”条载：“汤有三大辨十五辨。一日形辨，二日声辨，三日气辨。\n\n　　在泡行茶过程中，身体保持良好的姿态，头要正、肩要平，动作过程中眼神与动作要和谐自然，在泡茶过程中要沉肩、垂肘、提腕，要用手腕的起伏带动手的动作，切忌肘部高高抬起。\n\n　　茶文化知识\n\n　　茶叶的品质好坏，在没有科学仪器和方法鉴定的时候，可以通过色、香、形四个方面的来评价。而用这四个方面来评定茶叶质量的优劣，通常采用看、闻、摸、品进行鉴别。即看外形、色泽，闻香气，摸身骨，开汤品评。\n\n　　①色泽－－－－不同茶类有不同的色泽特点。绿茶中的炒青应呈黄绿色，烘青应呈深绿色蒸青应呈翠绿色，龙井则应在鲜绿色中略带米黄色；如果绿茶色泽灰暗、深褐，质量必定不佳。绿茶的汽色应呈浅绿或黄绿，清澈明亮；若为暗黄或混浊不清，也定不是好茶。红茶应乌黑油润，汤色红艳明亮，有些上品工夫红茶，其茶汤可在茶杯四周形成一圈黄色的油环，俗称“金圈”；若汤色时间暗淡，混浊不清，必是下等红茶。乌龙茶则以色泽青褐光润为好。\n\n　　②香气－－－－各类茶叶本身都有香味，如绿茶具清香，上品绿茶还有兰花香、板栗香等，红茶具清香及甜香或花香；乌龙茶具熟桃香等。若香气低沉，定为劣质茶；有陈气的为陈茶；有霉气等异味的为变质茶。就是苦丁茶，嗅起来也具有自然的香气。花茶则更以浓香吸引茶客。\n\n　　③口味－－－－或者叫茶叶的滋味，茶叶的本身滋味由苦、涩、甜、鲜、酸等多种成分构成。其成分比例得当，滋味就鲜醇可口，同时，不同的茶类，滋味也不一样，上等绿茶初尝有其苦涩感，但回味浓醇，令口舌生津；粗老劣茶则淡而无味，甚至涩口、麻舌。上等红茶滋味浓厚、强烈、鲜爽；低级红茶则平淡无味。苦丁茶入口是很苦的，但饮后口有回甜。\n\n　　④外形－－－－从茶叶的外形可以判断茶叶的品质，因为茶叶的好坏与茶采摘的鲜叶直接相关，也与制茶相关，这都反应在茶叶的外形上。如好的龙井茶，外形光、扁平、直，形似碗钉；好的珠茶，颗粒圆紧、均匀；好的工夫红茶条索紧齐，红碎茶颗粒齐整、划一；好的毛峰茶芽毫多、芽锋露等等。如果条索松散，颗粒松泡，叶表粗糙，身骨轻飘，就算不上是好茶了。\n\n　　茶叶冲泡按其程序可分为八道：\n\n　　1.百鹤沐浴（洗杯）：用开水洗净茶具；\n\n　　2.观音入宫（落茶）：把铁观音放入茶具，放茶量约占茶具容量的五分；\n\n　　3.悬壶高冲（冲茶）：把滚开的水提高冲入茶壶或盖瓯，使茶叶转动。\n\n　　4.春风拂面（乱泡沫）：用壶盖或瓯盖轻轻刮去漂浮的白泡沫，使其清新洁净；\n\n　　5.关公巡城（倒茶）：把泡一、二分钟后的茶水依次巡回注入并列的茶杯里；\n\n　　6韩信点兵（点茶）：茶水倒到少许时要一点一点均匀地滴到各茶杯里；\n\n　　7.鉴赏汤色（看茶）：观尝杯中茶水的颜色；\n\n　　8.吕啜甘霖（喝茶）：乘热细啜，先嗅其香，后尝其味，边啜边嗅，浅斟细饮。饮量虽不多，但能齿颊留香，唯底回甘，心旷神怡，别有情趣；\n\n', NULL, 'PUBLISHED', '2026-03-19 15:24:16', '2026-03-19 15:24:16');
INSERT INTO `material` VALUES (4, '饮食文化', '饮食器用', '      【五谷】\n\n　　古代所指的五种谷物。“五谷”，古代有多种不同说法，最主要的有两种：一种指稻、黍、稷、麦、菽；另一种指麻、黍、稷、麦、菽。两者的区别是：前者有稻无麻，后者有麻无稻。古代经济文化中心在黄河流域，稻的主要产地在南方，而北方种稻有限，所以“五谷”中最初无稻。\n\n　　【五牲】\n\n　　五种动物，具体所指说法不一：一种指牛、羊、猪、犬、鸡；一种指麋、鹿、磨、狼、兔；还有一种指磨、鹿、熊、狼、野猪。第一种说法流传较广。\n\n　　【五味】\n\n　　指酸、咸、甜(甘)、苦、辣(辛)五种味道。烹调上讲究“五味调和”。\n\n　　【六畜】\n\n　　指六种家畜：马、牛、羊、猪、狗、鸡。\n\n　　【八珍】\n\n　　指古代八种珍贵的食品。其具体所指随时代和地域而不同。陶宗仪《南村辍耕录》卷九云：“所谓八珍，则醍醐、麝沆、野驼蹄、鹿唇、驼乳麋、天鹅炙、紫玉浆、玄玉浆也。”后世以龙肝、凤髓、豹胎、鲤尾、?炙、猩唇、熊掌、酥酪蝉为八珍。\n\n　　【古代食器】\n\n　　古代食器种类很多，主要的有：簋(gui)，形似大碗，人们从?(yan)中盛出食物放在簋中再食用。?(fu)，是一种长方形的盛装食物的器具，用途与簋相同，故有“?簋对举”的说法。豆，像高脚盘，本用来盛黍稷，供祭祀用，后渐渐用来盛肉酱与肉羹了。皿，盛饭食的用具，两边有耳。盂，盛饮之器，敞口，深腹，有耳，下有圆形之足。盆盂，均为盛物之器。案，又称食案，是进食用的托盘，形体不大，有四足或三足，足很矮，古人进食时常“举案齐眉”，以示敬意。古人食肉常用匕把鼎中肉取出，置于俎上，然后用刀割着吃。匕，是长柄汤匙；俎，是长方形砧板，两端有足支地。古人常以刀匕、刀俎并举，并以“俎上肉”比喻受人欺凌、任人宰割的境遇。《鸿门宴》中有这么一句：“人为刀俎，我为鱼肉，何辞为?”说的就是这种境遇。箸，夹食的用具，与“住”谐音，含有停步之意，因避讳故取反义为“快”，又因以竹制成，故加个“竹”字头为“筷”，沿用至今。以上食器的质料均可选用竹、木、陶、青铜等。一般百姓大多用竹、木、陶制成，贵族的食器则以青铜居多。古代统治者所用的筷子，有的用金、银或象牙制成。\n\n　　【古代炊具】\n\n　　我国古代炊具有鼎、镬(huo)、甑(zeng)、?(yan)、鬲(li)等。鼎，最早是陶制的，殷周以后开始用青铜制作。鼎腹一般呈圆形，下有三足，故有“三足鼎立”之说；鼎的上沿有两耳，可穿进棍棒抬举。可在鼎腹下面烧烤。鼎的大小因用途不同而差别较大。古代常将整个动物放在鼎中烹煮，可见其容积较大。夏禹时的九鼎，经殷代传至周朝，象征国家最高权力，只有得到九鼎才能成为天子，可见它是传国之宝。镬是无足的鼎，与现在的大锅相仿，主要用来烹煮鱼肉之类的食物；后来它又发展成对犯人施行酷刑的工具，即将人投入镬中活活煮死。甑，是蒸饭的用具，与今之蒸笼、笼屉相似，最早用陶制成，后用青铜制作，其形直口立耳，底部有许多孔眼，置于鬲或釜上，甑里装上要蒸的食物，水煮开后，蒸气透过孔眼将食物蒸熟。鬲与鼎相近，但足空，且与腹相通，这是为了更大范围地接受传热，使食物尽快烂熟。鬲与甑合成一套使用称为“?”。鬲只用作炊具，故体积比鼎小。炊具可分为陶制、青铜制两大类。一般百姓多用陶制，青铜炊具为贵族所用。\n\n　　【古代酒器】\n\n　　尊，是古代酒器的通称，作为专名是一种盛酒器，敞口，高颈，圈足。尊上常饰有动物形象。壶，是一种长颈、大腹、圆足的盛酒器，不仅装酒，还能装水，故后代用“箪食壶浆”指犒劳军旅。彝、卣(you)、?(lei)、缶(fou)，都是形状不一的盛酒器。爵，古代饮酒器的总称，作为专名是用来温酒的，下有三足，可升火温酒。角，口呈两尖角形的饮酒器。觥，是一种盛酒、饮酒兼用的器具，像一只横放的牛角，长方圈足，有盖，多作兽形，觥常被用作罚酒，欧阳修《醉翁亭记》中有这样的描述：“射者中，奕者胜，觥筹交错，起坐而喧哗者，众宾欢也。”杯，椭圆形，是用来盛羹汤、酒、水的器物。杯的质料有玉、铜、银、瓷器，小杯为盏、盅。卮，也是一种盛酒器，《鸿门宴》中有“卮酒安足辞”之句。\n\n　　【羹】\n\n　　即肉汁。有两种：一种是纯肉汁，供食饮；另一种是肉羹，制成五味调和的浓肉汤，后泛指煮或蒸成的汁状、糊状、冻状的食品。在古代，肉是“肉食者”才能吃到的，贫苦百姓只能用白水煮菜为羹，这就是所谓的菜羹。\n\n　　【脍炙】\n\n　　脍，切细的鱼、肉；炙，烤肉。古代鲜肉一般用火炙，就像今天的烤羊肉串；干肉则用火烤。“食不厌精，脍不厌细”，可见古代脍食需要很高的刀工技法。脍炙，是人们所共同喜好的，后来把诗文为人所称颂叫做“脍炙人口”。\n\n　　【古代家具】\n\n　　我国古代家具主要有席、床、屏风、镜台、桌、椅、柜等。席子，是最古老、最原始的家具，最早由树叶编织而成，后来大都由芦苇、竹篾编成。古人常“席地而坐”，足见席子的应用是很广泛的。床，是席子以后最早出现的家具。一开始，床极矮，古人读书、写字、饮食、睡觉几乎都在床上进行。《孔雀东南飞》：“阿母得闻之，槌床便大怒。”诗中的“床”指的是坐具。和这种矮床配合用的家具有几、案、屏风等。还有一种矮榻常与床并用，故有“床榻”之称。魏晋南北朝以后，床的高度与今天的床差不多，成为专供睡觉的家具。唐宋以来，高型家具广泛普及，有床、桌、椅、凳、高几、长案、柜、衣架、巾架、屏风、盆架、镜台等，种类繁多，品种齐全。各个朝代的家具，都讲究工艺手法，力求图案丰富、雕刻精美，表现出浓厚的中国传统气派，成了我国传统文化的一个组成部分。其独特风格与样式，对世界不少国家产生过深远影响。\n\n', NULL, 'PUBLISHED', '2026-03-19 15:38:37', '2026-03-19 15:38:37');

-- ----------------------------
-- Table structure for material_category
-- ----------------------------
DROP TABLE IF EXISTS `material_category`;
CREATE TABLE `material_category`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `sort_order` int(0) NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_category
-- ----------------------------
INSERT INTO `material_category` VALUES (1, '历史沉淀', 1, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (2, '传统民俗', 2, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (3, '服饰装扮', 3, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (4, '行业手艺', 4, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (5, '宗教信仰', 5, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (6, '兵器武林', 6, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (7, '饮食文化', 7, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (8, '玉石珍宝', 8, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (9, '传说典故', 9, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (10, '科技文明', 10, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (11, '五行异象', 11, '2026-03-18 20:08:38', '2026-03-18 20:08:38');
INSERT INTO `material_category` VALUES (13, '', 100, '2026-03-19 01:10:58', '2026-03-19 01:10:58');
INSERT INTO `material_category` VALUES (14, '动作', 12, '2026-03-19 01:37:30', '2026-03-19 01:37:30');

-- ----------------------------
-- Table structure for material_favorite
-- ----------------------------
DROP TABLE IF EXISTS `material_favorite`;
CREATE TABLE `material_favorite`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `material_id` bigint(0) NOT NULL,
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '未定义' COMMENT '用户自定义收藏分组名称',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_material`(`user_id`, `material_id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_material`(`material_id`) USING BTREE,
  CONSTRAINT `fk_favorite_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_favorite
-- ----------------------------
INSERT INTO `material_favorite` VALUES (3, 4, 3, '未定义', '2026-03-19 15:24:49');

-- ----------------------------
-- Table structure for material_tag
-- ----------------------------
DROP TABLE IF EXISTS `material_tag`;
CREATE TABLE `material_tag`  (
  `material_id` bigint(0) NOT NULL,
  `tag_id` bigint(0) NOT NULL,
  PRIMARY KEY (`material_id`, `tag_id`) USING BTREE,
  INDEX `idx_tag_id`(`tag_id`) USING BTREE,
  CONSTRAINT `fk_mt_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_mt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '素材标签关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_tag
-- ----------------------------
INSERT INTO `material_tag` VALUES (3, 2);
INSERT INTO `material_tag` VALUES (3, 3);
INSERT INTO `material_tag` VALUES (4, 4);
INSERT INTO `material_tag` VALUES (4, 5);
INSERT INTO `material_tag` VALUES (4, 6);
INSERT INTO `material_tag` VALUES (4, 7);
INSERT INTO `material_tag` VALUES (4, 8);
INSERT INTO `material_tag` VALUES (4, 9);
INSERT INTO `material_tag` VALUES (4, 10);

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '接收者ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'material_approved/material_rejected',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `ref_id` bigint(0) NULL DEFAULT NULL COMMENT '关联素材ID',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_user_read`(`user_id`, `is_read`) USING BTREE,
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内通知' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 5, 'material_approved', '素材审核通过', '你的素材《一个好办法不是》已审核通过，现已收录至素材检索库！', 1, 1, '2026-03-14 18:38:21');
INSERT INTO `notification` VALUES (2, 4, 'material_rejected', '素材审核未通过', '你的素材《gfsbsrbwet》未通过审核。审核意见：gdhnrgnwr ', 2, 1, '2026-03-14 18:39:23');
INSERT INTO `notification` VALUES (3, 5, 'material_approved', '素材审核通过', '你的素材《这样？》已审核通过，现已收录至素材检索库！', 5, 0, '2026-03-19 01:05:44');

-- ----------------------------
-- Table structure for prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `dynasty` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `scene_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'scene/dialogue/fragment',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `template_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '占位符: {material_context} {user_input}',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dynasty_type`(`dynasty`, `scene_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提示词模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of prompt_template
-- ----------------------------
INSERT INTO `prompt_template` VALUES (1, '汉', 'scene', '汉代场景', '你是一位熟悉汉代历史的创作者。请根据以下素材与用户要求，用汉代风格描写场景。\n\n素材：{material_context}\n\n用户要求：{user_input}', '2026-03-08 22:43:20');
INSERT INTO `prompt_template` VALUES (2, '唐', 'dialogue', '唐代对话', '你是一位熟悉唐代文化的创作者。请根据以下素材，写一段唐代风格的人物对话。\n\n素材：{material_context}\n\n用户要求：{user_input}', '2026-03-08 22:43:20');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'ADMIN/CREATOR',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'ADMIN', '管理员');
INSERT INTO `role` VALUES (2, 'CREATOR', '普通创作者');

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '可选分类',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tag
-- ----------------------------
INSERT INTO `tag` VALUES (1, '茶文化，茶历史', NULL);
INSERT INTO `tag` VALUES (2, '茶文化', NULL);
INSERT INTO `tag` VALUES (3, '茶历史', NULL);
INSERT INTO `tag` VALUES (4, '五谷', NULL);
INSERT INTO `tag` VALUES (5, '五牲', NULL);
INSERT INTO `tag` VALUES (6, '五味', NULL);
INSERT INTO `tag` VALUES (7, '八珍', NULL);
INSERT INTO `tag` VALUES (8, '古代食器', NULL);
INSERT INTO `tag` VALUES (9, '古代酒器', NULL);
INSERT INTO `tag` VALUES (10, '古代家具', NULL);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SHA256或BCrypt',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `role_id` bigint(0) NOT NULL DEFAULT 2 COMMENT '1=ADMIN 2=CREATOR',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  `intro` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人简介',
  `security_question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `security_answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE,
  INDEX `idx_role`(`role_id`) USING BTREE,
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (4, 'master', '$2a$10$3hyECIV7A2XqFDZ0Q/prHOEzITkjqKR99kalh7pnn9Dc558ne0xCS', '1043997344@qq.com', '阿童木', NULL, 1, 1, '2026-03-08 23:03:36', '2026-03-08 23:41:20', NULL, '', '');
INSERT INTO `user` VALUES (5, 'moon', '$2a$10$xcpXlovYF.zgPGGRd7cGyu//YcKUxQmE5rqJa2XeK7rx8jXtymDqS', '', '', NULL, 2, 1, '2026-03-09 19:51:02', '2026-03-09 19:51:02', NULL, '我的OC是？', '韵律');

-- ----------------------------
-- Table structure for user_material
-- ----------------------------
DROP TABLE IF EXISTS `user_material`;
CREATE TABLE `user_material`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL COMMENT '创建者ID',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'draft' COMMENT 'draft=草稿/pending=待审核/approved=已通过/rejected=已拒绝',
  `admin_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `favorite_group` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '我的灵感' COMMENT '用户自定义收藏分组名称',
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  CONSTRAINT `fk_user_material_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户自建素材' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_material
-- ----------------------------
INSERT INTO `user_material` VALUES (5, 5, '历史沉淀', '这样？', '十九点', '', 'approved', 'butongg ', '我的灵感', '2026-03-19 01:02:36', '2026-03-19 01:02:36');
INSERT INTO `user_material` VALUES (6, 5, '历史沉淀', '上的', '但是', '', 'draft', NULL, '我的灵感', '2026-03-19 01:02:53', '2026-03-19 01:02:53');

-- ----------------------------
-- Table structure for verification_code
-- ----------------------------
DROP TABLE IF EXISTS `verification_code`;
CREATE TABLE `verification_code`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `target` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号或邮箱',
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'register/login/reset',
  `expires_at` datetime(0) NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_target_type`(`target`, `type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '验证码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of verification_code
-- ----------------------------

-- ----------------------------
-- Table structure for work_group
-- ----------------------------
DROP TABLE IF EXISTS `work_group`;
CREATE TABLE `work_group`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_name`(`user_id`, `name`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_work_group_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '作品分组' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of work_group
-- ----------------------------
INSERT INTO `work_group` VALUES (1, 9, '离开', '2026-03-16 22:48:22', '2026-03-16 22:48:22');
INSERT INTO `work_group` VALUES (2, 9, '行吧', '2026-03-17 15:43:46', '2026-03-17 15:43:46');
INSERT INTO `work_group` VALUES (3, 9, 'gh', '2026-03-17 15:59:55', '2026-03-17 15:59:55');
INSERT INTO `work_group` VALUES (4, 8, 'dskjlks', '2026-03-17 18:17:24', '2026-03-17 18:17:24');
INSERT INTO `work_group` VALUES (5, 4, '茶色生香', '2026-03-19 15:35:31', '2026-03-19 15:35:31');

SET FOREIGN_KEY_CHECKS = 1;
