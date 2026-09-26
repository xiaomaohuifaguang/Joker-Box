"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
add${tableNameUp},
get${tableNameUp}Info,
update${tableNameUp},
} from "@/lib/api/${tableNameDown}";
import { ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import {
Dialog,
DialogContent,
DialogDescription,
DialogFooter,
DialogHeader,
DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import type { ${tableNameUp} } from "@/types";

const EMPTY: ${tableNameUp} = {
<#list fieldInfos as field>
    ${field.fieldName}: <#if field.tsType == "number">0<#elseif field.tsType == "boolean">false<#else>""</#if>,
</#list>
};

// 返回后回填 description+prompt。两字段均必填。
export function ${tableNameUp}FormDialog({
open,
onOpenChange,
editing,
onSuccess,
}: {
open: boolean;
onOpenChange: (open: boolean) => void;
editing: ${tableNameUp} | null;
onSuccess: () => void;
}) {
const [form, setForm] = useState<${tableNameUp}>(EMPTY);
    const [busy, setBusy] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    // 详情加载失败标记：失败时不展示空表单（防覆盖已有提示词），给重试入口。
    const [infoError, setInfoError] = useState(false);
    // 重试计数：+1 触发 effect 重新拉详情。
    const [reloadKey, setReloadKey] = useState(0);

    const editingId = editing?.${primaryKey.fieldName} ?? null;
    const [prev, setPrev] = useState<{ open: boolean; ${primaryKey.fieldName}: ${primaryKey.tsType} | null }>({
    open: false,
    ${primaryKey.fieldName}: null,
    });
    if (prev.open !== open || prev.${primaryKey.fieldName} !== editingId) {
    setPrev({ open, ${primaryKey.fieldName}: editingId });
    if (open) {
    // 编辑：先 loading（清旧值防闪现），effect 异步拉详情回填；新增：直接空表单。
    setDetailLoading(!!editing);
    setInfoError(false);
    setForm(EMPTY);
    }
    }

    useEffect(() => {
    if (!open || editing == null || editingId == null) return;
    let cancelled = false;
    get${tableNameUp}Info(editing)
    .then((d) => {
    if (cancelled) return;
    setForm(d);
    })
    .catch((err) => {
    if (!cancelled) {
    toast.error(err instanceof ApiError ? err.message : "加载详情失败");
    setInfoError(true);
    }
    })
    .finally(() => {
    if (!cancelled) setDetailLoading(false);
    });
    return () => {
    cancelled = true;
    };
    }, [open, editingId, reloadKey]);

    function set<K extends keyof ${tableNameUp}>(
        key: K,
        value: ${tableNameUp}[K],
        ) {
        setForm((f) => ({ ...f, [key]: value }));
        }

        async function submit() {

        setBusy(true);
        try {
        if (editing) {
        await update${tableNameUp}(form);
        toast.success("已保存");
        } else {
        await add${tableNameUp}(form);
        toast.success("已新增");
        }
        onOpenChange(false);
        onSuccess();
        } catch (err) {
        toast.error(err instanceof ApiError ? err.message : "操作失败");
        } finally {
        setBusy(false);
        }
        }

        return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>{editing ? "编辑" : "新增"}</DialogTitle>
                    <DialogDescription>
                        {editing ? "修改此数据" : "新建一条数据"}
                    </DialogDescription>
                </DialogHeader>

                {detailLoading ? (
                <div className="flex flex-col gap-4">
                    {Array.from({ length: 3 }).map((_, i) => (
                    <Skeleton key={i} className="h-9 w-full" />
                    ))}
                </div>
                ) : infoError ? (
                // 详情加载失败：不展示空表单，避免保存时覆盖已有提示词。
                <div className="flex flex-col items-center gap-3 py-6">
                    <p className="text-sm text-muted-foreground">加载提示词详情失败</p>
                    <Button
                            variant="outline"
                            onClick={() => {
                    setInfoError(false);
                    setDetailLoading(true);
                    setReloadKey((k) => k + 1);
                    }}
                    >
                    重试
                    </Button>
                </div>
                ) : (
                <div className="flex flex-col gap-4">
                    <#list fieldInfos as field>
                        <div className="flex flex-col gap-2">
                            <Label className="text-sm text-muted-foreground">${field.fieldName}</Label>
                            <Input
                                    value={form.${field.fieldName}}
                                    onChange={(e) => set("${field.fieldName}", <#if field.tsType == "number">Number(e.target.value)<#else>e.target.value</#if>)}
                                    <#if field.tsType == "number">type={"number"}<#else>type={"text"}</#if>
                            placeholder="${field.comment}"
                            />
                        </div>
                    </#list>
                </div>
                )}

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)}>
                    取消
                    </Button>
                    <Button
                            onClick={submit}
                            disabled={busy || detailLoading || infoError}
                    >
                        {busy ? "保存中…" : "保存"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
        );
        }
