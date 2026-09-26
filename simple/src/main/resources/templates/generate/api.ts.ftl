import { api } from "@/lib/api";
import type {
    ${tableNameUp},
    ${tableNameUp}PageParam,
    Page
} from "@/types";

/** ${tableComment}分页查询 */
export async function query${tableNameUp}Page(
    params: ${tableNameUp}PageParam,
): Promise<Page<${tableNameUp}>> {
    const { data } = await api.post<Page<${tableNameUp}>>("/${tableNameDown}/queryPage", {
        body: params,
    });
    return data;
}

/** ${tableComment}新增 */
export async function add${tableNameUp}(
    ${tableNameDown}: ${tableNameUp}
): Promise<void> {
    await api.post<unknown>("/${tableNameDown}/add", { body: ${tableNameDown} });
}

/** ${tableComment}删除 */
export async function remove${tableNameUp}(
    ${tableNameDown}: ${tableNameUp}
): Promise<void> {
    await api.post<unknown>("/${tableNameDown}/remove", { body: ${tableNameDown} });
}

/** ${tableComment}修改 */
export async function update${tableNameUp}(
    ${tableNameDown}: ${tableNameUp}
): Promise<void> {
    await api.post<unknown>("/${tableNameDown}/update", { body: ${tableNameDown} });
}

 /** ${tableComment}获取详情 */
export async function get${tableNameUp}Info(
    ${tableNameDown}: ${tableNameUp}
): Promise<${tableNameUp}> {
    const { data } = await api.post<${tableNameUp}>("/${tableNameDown}/info", { body: ${tableNameDown} });
    return data;
}