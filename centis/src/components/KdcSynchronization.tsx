export default function KdcSynchronization() {
    return (
        <fieldset className="my-4 d-flex flex-wrap h-25 form-control shadow-sm rounded-0 p-3">
            <legend>KDC Synchronization</legend>
            <div>
                <p>
                    If one of the KDCs fails or has to be restarted, the keys have to be synchronized to reduce errors.
                </p>
                <p>
                    Synchronization performs <code>getState</code> request gathering the oldest keys of each KDC
                    followed by <code>setState</code> with the gathered keys. If a KDC has another KDC's oldest key then it must delete all keys that are
                    older than
                    it.
                </p>
            </div>
            <div className="d-flex flex-row justify-content-end w-100 my-3">
                <button className="btn btn-sm btn-outline-primary">Synchronize Keys</button>
            </div>
        </fieldset>
    )
}