package com.xlxyvergil.fusedcritical.config;

// L2 Tabs 属性面板中暴击率 / 暴击伤害的显示方式，对应配置项 l2TabsCritDisplayMode
public enum L2TabsDisplayMode {

    // 用归一化接收方属性替换面板原有的两行
    REPLACE,

    // 保留面板原有两行，并在其后追加归一化接收方属性
    APPEND,

    // 不改动面板，保持 L2 Tabs / l2damagetracker 原样
    NONE
}
