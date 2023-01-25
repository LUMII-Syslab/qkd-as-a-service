export default function WatchKeys() {
    return (
        <>
            {/*create input to set delay for key requests*/}
            <div className={"d-flex flex-row  mb-3 col-12"}>
                <div className="form-floating col-2">
                    <input type="number" id="wk-delay" placeholder="Aija URL"
                           className="form-control"
                           defaultValue={0}/>
                    <label htmlFor="wk-delay">request delay in ms</label>
                </div>
                <input type="button" id="toggle-monitor" value="start watching keys"
                       className="btn ms-4 btn-primary col-2"/>
            </div>
            <table className="table table-bordered">
                <colgroup>
                    <col span={1} style={{width: "30%"}}/>
                    <col span={1} style={{width: "15%"}}/>
                    <col span={1} style={{width: "15%"}}/>
                    <col span={1} style={{width: "20%"}}/>
                    <col span={1} style={{width: "20%"}}/>
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
                <tbody id="my-table"></tbody>
            </table>
        </>);
}