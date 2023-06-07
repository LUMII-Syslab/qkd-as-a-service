import { useEffect, useRef, useState } from "react";
import {
  bytesToHexOctets,
  bytesToSpacedHexOctets,
} from "../utils/formatting-bytes";
import { wsConnect, wsSendRequest } from "../utils/promise-ws";
import {
  encodeReserveKeyRequest,
  decodeReserveKeyResponse,
  ReserveKeyRequest,
} from "../utils/reserve-key-req";
import {
  encodeGetKeyRequest,
  decodeGetKeyResponse,
  GetKeyRequest,
  GetKeyResponse,
} from "../utils/get-key-req";
import {
    errorIds
} from '../utils/translate-ids'
interface WatchKeysTableRow {
  ErrId: number;
  KeyId: Uint8Array;
  Left: Uint8Array;
  Right: Uint8Array;
  HashLeft: Uint8Array;
  HashRight: Uint8Array;
}

export default function WatchKeys({ config }) {
  let [watchingKeys, setWatchingKeys] = useState(false);
  let [requestDelay, setRequestDelay] = useState(50);
  let [aijaWS, setAijaWS] = useState(null);
  let [brencisWS, setBrencisWS] = useState(null);
  let interval = useRef(null);
  let [tableRowCount, setTableRowCount] = useState(5);

  let [tableRows, setTableRows] = useState([] as WatchKeysTableRow[]);

  useEffect(() => {
    wsConnect(config.aijaEndpoint).then((ws) => {
      setAijaWS(ws);
    });
    wsConnect(config.brencisEndpoint).then((ws) => {
      setBrencisWS(ws);
    });
  }, [config.aijaEndpoint, config.brencisEndpoint, watchingKeys]);

  useEffect(() => {
    clearInterval(interval.current);
    interval.current = setInterval(async () => {
      if (!watchingKeys) return;

      let result = {} as WatchKeysTableRow;

      let rkaghReq = encodeReserveKeyRequest({
        keyLength: 256,
        cNonce: 12345,
      } as ReserveKeyRequest);

      let rkaghResp = decodeReserveKeyResponse(
        await wsSendRequest(aijaWS, rkaghReq)
      );

      let gkhReq = encodeGetKeyRequest({
        keyId: bytesToHexOctets(rkaghResp.keyId),
        cNonce: 12345,
        keyLength: 256,
      } as GetKeyRequest);

      let gkhResp: GetKeyResponse = decodeGetKeyResponse(
        await wsSendRequest(brencisWS, gkhReq)
      );

      result.ErrId = gkhResp.errorId;
      result.KeyId = rkaghResp.keyId;
      result.Left = rkaghResp.thisHalf;
      result.Right = gkhResp.thisHalf;
      result.HashLeft = gkhResp.otherHash;
      result.HashRight = rkaghResp.otherHash;

      tableRows.unshift(result);
      while (tableRows.length > tableRowCount) tableRows.pop();
      setTableRows([...tableRows]);
    }, requestDelay);
  }, [
    aijaWS,
    brencisWS,
    config,
    requestDelay,
    tableRowCount,
    tableRows,
    watchingKeys,
  ]);

  return (
    <div className="border shadow-sm p-3 my-3 bg-white">
      <div className={"d-flex flex-row  mb-3 col-12"}>
        <div className="form-floating col-3">
          <input
            type="number"
            id="wk-delay"
            className="form-control"
            defaultValue={requestDelay}
            onChange={(e) => {
              setRequestDelay(parseInt(e.target.value));
            }}
          />
          <label htmlFor="wk-delay">request delay in ms</label>
        </div>
        <div className="form-floating col-2 mx-3">
          <input
            type="number"
            className="form-control"
            defaultValue={tableRowCount}
            onChange={(e) => {
              setTableRowCount(parseInt(e.target.value));
            }}
          />
          <label>table row count</label>
        </div>
        <input
          type="button"
          id="toggle-monitor"
          value={watchingKeys ? "Stop Monitoring" : "Start Monitoring"}
          className="btn btn-outline-primary btn-sm ms-1"
          onClick={() => {
            setWatchingKeys(!watchingKeys);
          }}
        />
      </div>
      <table className="table table-bordered">
        <colgroup>
          <col span={1} style={{ width: "30%" }} />
          <col span={1} style={{ width: "15%" }} />
          <col span={1} style={{ width: "15%" }} />
          <col span={1} style={{ width: "20%" }} />
          <col span={1} style={{ width: "20%" }} />
        </colgroup>
        <thead>
          <tr>
            <th>key id</th>
            <th>left</th>
            <th>right</th>
            <th>hash(left)</th>
            <th>hash(right)</th>
          </tr>
        </thead>
        <tbody className="text-center">
          {tableRows.map((row, i) => {
            if (row.ErrId === 0) {
              return (
                <tr key={i}>
                  <td>
                    <code>{bytesToSpacedHexOctets(row.KeyId)}</code>
                  </td>
                  <td>
                    <code>{bytesToSpacedHexOctets(row.Left)}</code>
                  </td>
                  <td>
                    <code>{bytesToSpacedHexOctets(row.Right)}</code>
                  </td>
                  <td>
                    <code>{bytesToSpacedHexOctets(row.HashLeft)}</code>
                  </td>
                  <td>
                    <code>{bytesToSpacedHexOctets(row.HashRight)}</code>
                  </td>
                </tr>
              );
            } else {
              return (
                <tr key={i} className="bg-danger bg-opacity-25">
                  <td colSpan={5}>
                    error: {row.ErrId} ({errorIds[row.ErrId]})
                  </td>
                </tr>
              );
            }
          })}
        </tbody>
      </table>
    </div>
  );
}
