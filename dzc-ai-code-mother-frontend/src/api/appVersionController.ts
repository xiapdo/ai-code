// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /version/get/${param0} */
export async function getVersionDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVersionDetailParams,
  options?: { [key: string]: any }
) {
  const { versionId: param0, ...queryParams } = params
  return request<API.BaseResponseAppVersionSnapshot>(`/version/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /version/list/page */
export async function listVersionByPage(
  body: API.VersionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVersionSnapshotVO>('/version/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /version/rollback */
export async function rollbackVersion(
  body: API.VersionRollbackRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/version/rollback', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
