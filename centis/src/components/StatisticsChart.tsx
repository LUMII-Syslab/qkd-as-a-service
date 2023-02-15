import Chart, {ChartItem} from 'chart.js/auto';
import {useContext, useEffect, useRef} from "react";
import {ConfigContext} from "../utils/config-context";
import {wsConnect, wsSendRequest} from "../utils/promise-ws";
import {decodeGetStateResponse, encodeGetStateRequest, GetStateRequest} from "../utils/get-state-req";

export default function StatisticsChart() {
    const config = useContext(ConfigContext)

    let chartCanvas = useRef();

    useEffect(() => {
        let chart = new Chart(chartCanvas.current as ChartItem, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Aija reservable Keys',
                    data: [],
                    tension: 0.3,
                },
                    {
                        label: 'Brencis reservable Keys',
                        data: [],
                        tension: 0.3,

                    }
                ]
            },
            options: {
                animation:{
                    duration: 0
                },
                scales: {
                    x: {
                        ticks: {
                            autoSkip: true,
                            maxTicksLimit: 5,
                        },
                    }
                }
            },
        });
        let interval = setInterval(async () => {
            let aijaWs = await wsConnect(config.aijaEndpoint) as WebSocket
            let brencisWS = await wsConnect(config.brencisEndpoint) as WebSocket

            let encodedRequest = encodeGetStateRequest({cNonce: Math.floor(Math.random() * 100000)} as GetStateRequest)

            let aijaResponse = decodeGetStateResponse(await wsSendRequest(aijaWs, encodedRequest))
            let brencisResponse = decodeGetStateResponse(await wsSendRequest(brencisWS, encodedRequest))

            aijaWs.close()
            brencisWS.close()

            console.log(aijaResponse)
            console.log(brencisResponse)

            chart.data.labels.push(new Date().toLocaleString('sv'))
            chart.data.datasets[0].data.push(aijaResponse.reservable)
            chart.data.datasets[1].data.push(brencisResponse.reservable)
            chart.update()
        }, 500)
        return () => {
            clearInterval(interval)
            chart.destroy();
        }
    }, [])

    return (
        <div className="my-4">
            <canvas ref={chartCanvas}></canvas>
        </div>
    )
}